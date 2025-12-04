// Replace the verify(...) method in VoteSubmissionServiceImplementation with the code below.
// This ensures the recompute event is published AFTER the surrounding transaction commits,
// so the VoteTally recompute will operate on committed VERIFIED submissions.
// It uses TransactionSynchronizationManager to schedule the publish in afterCommit.
// The rest of the method (notifications, audit log) remains unchanged.

@Override
@Transactional
public VoteSubmissionDto verify(UUID id, VoteSubmissionVerifyRequest req) {
    VoteSubmission s = voteSubmissionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));

    SystemUser verifier = userRepo.findById(req.getVerifierUserId())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Verifier not found"));

    if (s.getStatus() != VoteStatus.PENDING) {
        throw new ResponseStatusException(BAD_REQUEST, "Submission already processed");
    }

    boolean accept = Boolean.TRUE.equals(req.getAccept());
    s.setStatus(accept ? VoteStatus.VERIFIED : VoteStatus.REJECTED);
    s.setVerifiedBy(verifier);
    s.setDateVerified(LocalDateTime.now());

    if (req.getComment() != null && !req.getComment().isBlank()) {
        String prefix = (s.getComments() == null ? "" : s.getComments() + "\n");
        s.setComments(prefix + "[review] " + req.getComment());
    }

    VoteSubmission saved = voteSubmissionRepository.save(s);

    // Ensure recompute event is published AFTER the current transaction commits.
    // This avoids the recompute reading data that hasn't been committed yet.
    // If TransactionSynchronization is not active, fall back to immediate publish.
    final RecomputeEvent recomputeEvent = new RecomputeEvent(this,
            saved.getOrganization().getOrgId(),
            saved.getElection().getElectionId(),
            verifier.getUserId());

    try {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                // publish after commit; listener should be @Async to avoid blocking caller
                                eventPublisher.publishEvent(recomputeEvent);
                            } catch (Exception ex) {
                                // log and continue: do NOT fail the HTTP response due to recompute publish failure
                                // Use whatever logger instance your class has (SLF4J)
                                log.warn("Failed to publish RecomputeEvent after commit: {}", ex.getMessage());
                            }
                        }
                    }
            );
        } else {
            // No transaction synchronization active — publish immediately (fallback)
            eventPublisher.publishEvent(recomputeEvent);
        }
    } catch (Exception ex) {
        // Defensive: don't fail verification if scheduling the event fails
        log.warn("Could not schedule recompute event: {}", ex.getMessage());
    }

    if (saved.getStatus() == VoteStatus.VERIFIED) {
        notify(
                saved.getOrganization().getOrgId(), saved.getAgent().getUserId(),
                NotificationType.VOTE,
                "Submission Verified",
                "Your submission at " + saved.getPollingCenter().getCenterName() + " was verified.",
                "vote_submission", saved.getSubmissionId(),
                NotificationPriority.NORMAL, DeliveryMethod.IN_APP
        );

        auditLogService.logSubmissionVerify(
                saved.getOrganization().getOrgId(),
                verifier.getUserId(),
                "VoteSubmission",
                "Verified submission: " + saved.getSubmissionId()
        );
    } else {
        String message = "Your submission at " + saved.getPollingCenter().getCenterName() + " was rejected.";
        if (req.getComment() != null && !req.getComment().isBlank()) {
            message += " Reason: " + req.getComment();
        }
        notify(
                saved.getOrganization().getOrgId(), saved.getAgent().getUserId(),
                NotificationType.VOTE,
                "Submission Rejected",
                message,
                "vote_submission", saved.getSubmissionId(),
                NotificationPriority.NORMAL, DeliveryMethod.IN_APP
        );

        auditLogService.logSubmissionReject(
                saved.getOrganization().getOrgId(),
                verifier.getUserId(),
                "VoteSubmission",
                "Rejected submission: " + saved.getSubmissionId() +
                        (req.getComment() != null && !req.getComment().isBlank()
                                ? " Reason: " + req.getComment()
                                : "")
        );
    }

    return mapper.toDTO(saved);
}