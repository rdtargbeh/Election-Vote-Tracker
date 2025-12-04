package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.*;
import Backend.ElectionVote.entity.*;
import Backend.ElectionVote.enums.DeliveryMethod;
import Backend.ElectionVote.enums.NotificationPriority;
import Backend.ElectionVote.enums.NotificationType;
import Backend.ElectionVote.enums.VoteStatus;
import Backend.ElectionVote.mapper.VoteSubmissionMapper;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.integration.SigningService;
import Backend.ElectionVote.service.*;
import Backend.ElectionVote.utility.RecomputeEvent;
import Backend.ElectionVote.utility.RequestUtils;
import Backend.ElectionVote.utility.VoteSubmissionSpecs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

import static org.springframework.http.HttpStatus.*;

/**
 * VoteSubmissionServiceImplementation
 *
 * Responsibilities:
 * - Validate submission payloads (counts, allocation)
 * - Persist submissions with duplicate detection (submissionHash) and optional idempotency key
 * - Attach files (tally sheets)
 * - Create an audit_ledger entry and record chain_hash, then sign chain_hash (via SigningService)
 * - Support updates with optimistic-lock retry
 * - Verify submissions (mark VERIFIED/REJECTED) and trigger tally recompute (synchronous here)
 *
 * Notes about changes vs previous implementation:
 * - Adds optimistic locking handling and retry in update() to reduce lost-update conflicts.
 * - Adds atomic ledger insertion + chain_hash update using fn_log_ledger_and_update_submission via JdbcTemplate,
 *   then signs the chain_hash via a SigningService (KMS-backed implementation expected in prod).
 * - Keeps existing validation, file handling, notifications and audit_log calls.
 *
 * Operational note:
 * - For heavy election-day load, consider moving recomputeForElection to an async worker to avoid long transactions.
 */

@Service
@RequiredArgsConstructor
public class VoteSubmissionServiceImplementation implements VoteSubmissionService {

    private final EntityManager em;

    private final VoteSubmissionRepository voteSubmissionRepository;
    private final OrganizationRepository orgRepo;
    private final ElectionRepository electionRepo;
    private final SystemUserRepository userRepo;
    private final VoteTallyRepository voteTallyRepository;
    private final VoteTallyService voteTallyService;
    private final NotificationService notificationService;

    private final PollingCenterRepository centerRepo;
    private final PollingPlaceRepository placeRepo;
    private final PollingPlaceAllocationRepository placeAllocationRepo;
    private final PollingCenterAllocationRepository allocationRepo;

    private final FileUploadService fileUploadService;
    private final TallySheetRepository tallySheetRepository;
    private final AuditLogService auditLogService;

    private final VoteSubmissionMapper mapper;

    // geometry factory for SRID 4326 (lon/lat)
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final Logger log = LoggerFactory.getLogger(VoteTallyServiceImplementation.class);

    private static final int OPTIMISTIC_LOCK_RETRIES = 3;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbc;
    private final SigningService signingService;
    private final ApplicationEventPublisher eventPublisher;
    private static final int MAX_CANDIDATE_KEYS = 2000; // throttle very large payloads



    @Override
    @Transactional
    public VoteSubmissionDto create(VoteSubmissionCreateRequest req, List<MultipartFile> files, HttpServletRequest request) {
        return createInternal(req, files, request);
    }

    @Override
    @Transactional
    public VoteSubmissionDto create(VoteSubmissionCreateRequest req, HttpServletRequest request) {
        return createInternal(req, null, request);
    }

    private VoteSubmissionDto createInternal(VoteSubmissionCreateRequest req, List<MultipartFile> files, HttpServletRequest request) {
        Organization org = orgRepo.findById(req.getOrgId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));

        Election e = electionRepo.findById(req.getElectionId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Election not found"));

        PollingCenter c = centerRepo.findById(req.getCenterId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling center not found"));

        SystemUser agent = userRepo.findById(req.getAgentId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Agent not found"));

        PollingPlace p = placeRepo.findById(req.getPlaceId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling place not found"));

        if (!p.getPollingCenter().getCenterId().equals(c.getCenterId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Polling place does not belong to the specified polling center");
        }

        var alloc = placeAllocationRepo
                .findByElection_ElectionIdAndPollingPlace_PlaceId(e.getElectionId(), p.getPlaceId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Polling place not allocated for this election"));

        if (req.getCandidateVotes() != null && req.getCandidateVotes().size() > MAX_CANDIDATE_KEYS) {
            throw new ResponseStatusException(BAD_REQUEST, "Too many candidate entries");
        }

        validateTally(
                req.getCandidateVotes(),
                nz(req.getInvalidBallots()),
                nz(req.getBlankBallots()),
                nz(req.getRejectedBallots()),
                nz(req.getSpoiledBallots()),
                nz(req.getBallotsCast()),
                alloc.getRegisteredVoters(),
                alloc.getBallotsIssued()
        );

        VoteSubmission s = mapper.toEntity(req, org, e, c, agent);
        s.setPollingPlace(p);

        if (request != null) {
            s.setClientIp(RequestUtils.getClientIp(request));
            s.setUserAgent(RequestUtils.getUserAgent(request));
        }

        if (req.getLatitude() != null && req.getLongitude() != null) {
            Point gps = geometryFactory.createPoint(new Coordinate(req.getLongitude(), req.getLatitude()));
            gps.setSRID(4326);
            s.setGpsLocation(gps);
        }

        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            voteSubmissionRepository.findByIdempotencyKey(req.getIdempotencyKey()).ifPresent(existing -> {
                throw new ResponseStatusException(CONFLICT, "Submission with this idempotency key already exists: " + existing.getSubmissionId());
            });
            s.setIdempotencyKey(req.getIdempotencyKey());
        }

        s.setSubmissionHash(buildSubmissionHash(
                org.getOrgId(),
                e.getElectionId(),
                c.getCenterId(),
                p.getPlaceId(),
                agent.getUserId(),
                s.getCandidateVotes(),
                s.getBallotsCast(),
                s.getInvalidBallots(),
                s.getBlankBallots(),
                s.getRejectedBallots(),
                s.getSpoiledBallots()
        ));

        if (voteSubmissionRepository.existsBySubmissionHash(s.getSubmissionHash())) {
            throw new ResponseStatusException(CONFLICT, "Duplicate submission (same content).");
        }

        VoteSubmission saved = voteSubmissionRepository.save(s);

        if (files != null && !files.isEmpty()) {
            attachFilesToSubmission(org, saved, agent, files);
        }

        // Ledger (atomic) + sign
        String payloadHash = buildSubmissionPayloadHash(saved);
        Map<String, Object> ledgerRes = jdbc.queryForMap(
                "SELECT * FROM fn_log_ledger_and_update_submission(?, ?, ?, ?)",
                "VOTE_SUBMISSION",
                saved.getSubmissionId(),
                payloadHash,
                agent.getUserId()
        );

        UUID ledgerId = toUuid(ledgerRes.get("ledger_id"));
        String chainHash = ledgerRes.get("chain_hash") != null ? ledgerRes.get("chain_hash").toString() : null;

        SigningService.SignResult signResult = signingService.signHex(chainHash);

        jdbc.update("UPDATE audit_ledger SET signature = ? WHERE ledger_id = ?", signResult.signature(), ledgerId);
        jdbc.update("UPDATE vote_submission SET submission_signature = ?, submission_signer_key_id = ? WHERE submission_id = ?",
                signResult.signature(), signResult.keyId(), saved.getSubmissionId());

        auditLogService.logSubmissionCreate(
                org.getOrgId(),
                agent.getUserId(),
                "VoteSubmission",
                "Created submission: " + saved.getSubmissionId() +
                        " at " + c.getCenterName() +
                        " / place: " + p.getCode()
        );

        String placeDisplay = (p.getLabel() != null && !p.getLabel().isBlank())
                ? p.getLabel()
                : "Place " + p.getPlaceNumber();

        notify(
                org.getOrgId(), agent.getUserId(),
                NotificationType.VOTE,
                "Submission Received",
                "Your vote submission for " + c.getCenterName() + " - " + placeDisplay + " was received.",
                "vote_submission", saved.getSubmissionId(),
                NotificationPriority.NORMAL, DeliveryMethod.IN_APP
        );

        return mapper.toDTO(saved);
    }


    @Override
    @Transactional
    public VoteSubmissionDto update(UUID id, VoteSubmissionUpdateRequest req, List<MultipartFile> files) {
        int attempts = 0;
        while (true) {
            try {
                VoteSubmission s = voteSubmissionRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));

                if (s.getStatus() != VoteStatus.PENDING) {
                    throw new ResponseStatusException(BAD_REQUEST, "Only PENDING submissions can be updated");
                }

                PollingPlace place = s.getPollingPlace();
                if (place == null) throw new ResponseStatusException(BAD_REQUEST, "Submission is missing polling place reference");

                var alloc = placeAllocationRepo
                        .findByElection_ElectionIdAndPollingPlace_PlaceId(
                                s.getElection().getElectionId(),
                                place.getPlaceId()
                        )
                        .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Polling place not allocated for this election"));

                Map<UUID, Integer> mergedVotes =
                        (req.getCandidateVotes() != null)
                                ? req.getCandidateVotes()
                                : (s.getCandidateVotes() != null ? s.getCandidateVotes() : Collections.emptyMap());

                int cast    = (req.getBallotsCast()    != null) ? req.getBallotsCast()    : s.getBallotsCast();
                int invalid = (req.getInvalidBallots() != null) ? req.getInvalidBallots() : s.getInvalidBallots();
                int blank   = (req.getBlankBallots()   != null) ? req.getBlankBallots()   : s.getBlankBallots();
                int rej     = (req.getRejectedBallots()!= null) ? req.getRejectedBallots(): s.getRejectedBallots();
                int spo     = (req.getSpoiledBallots() != null) ? req.getSpoiledBallots() : s.getSpoiledBallots();

                validateTally(mergedVotes, invalid, blank, rej, spo, cast,
                        alloc.getRegisteredVoters(), alloc.getBallotsIssued());

                mapper.apply(req, s);

                s.setSubmissionHash(buildSubmissionHash(
                        s.getOrganization().getOrgId(),
                        s.getElection().getElectionId(),
                        s.getPollingCenter().getCenterId(),
                        s.getPollingPlace().getPlaceId(),
                        s.getAgent().getUserId(),
                        s.getCandidateVotes(),
                        s.getBallotsCast(),
                        s.getInvalidBallots(),
                        s.getBlankBallots(),
                        s.getRejectedBallots(),
                        s.getSpoiledBallots()
                ));

                VoteSubmission saved = voteSubmissionRepository.save(s);

                if (files != null && !files.isEmpty()) {
                    attachFilesToSubmission(saved.getOrganization(), saved, saved.getAgent(), files);
                }

                // Ledger update + sign
                String payloadHash = buildSubmissionPayloadHash(saved);
                Map<String, Object> ledgerRes = jdbc.queryForMap(
                        "SELECT * FROM fn_log_ledger_and_update_submission(?, ?, ?, ?)",
                        "VOTE_SUBMISSION_UPDATE",
                        saved.getSubmissionId(),
                        payloadHash,
                        saved.getAgent().getUserId()
                );
                UUID ledgerId = toUuid(ledgerRes.get("ledger_id"));
                String chainHash = ledgerRes.get("chain_hash") != null ? ledgerRes.get("chain_hash").toString() : null;

                SigningService.SignResult signResult = signingService.signHex(chainHash);

                jdbc.update("UPDATE audit_ledger SET signature = ? WHERE ledger_id = ?", signResult.signature(), ledgerId);
                jdbc.update("UPDATE vote_submission SET submission_signature = ?, submission_signer_key_id = ? WHERE submission_id = ?",
                        signResult.signature(), signResult.keyId(), saved.getSubmissionId());

                auditLogService.logSubmissionUpdate(
                        saved.getOrganization().getOrgId(),
                        saved.getAgent().getUserId(),
                        "VoteSubmission",
                        "Updated submission: " + saved.getSubmissionId()
                );

                notify(
                        saved.getOrganization().getOrgId(), saved.getAgent().getUserId(),
                        NotificationType.VOTE,
                        "Submission Updated",
                        "Your vote submission for " + saved.getPollingCenter().getCenterName() + " was updated.",
                        "vote_submission", saved.getSubmissionId(),
                        NotificationPriority.LOW, DeliveryMethod.IN_APP
                );

                return mapper.toDTO(saved);

            } catch (ObjectOptimisticLockingFailureException e) {
                attempts++;
                if (attempts > OPTIMISTIC_LOCK_RETRIES) {
                    throw new ResponseStatusException(CONFLICT, "Concurrent update conflict, please retry");
                }
                try { Thread.sleep(50L + (long)(Math.random()*50)); } catch (InterruptedException ignored) {}
            }
        }
    }


    // ----------------- VERIFY (unchanged notifications) -----------------

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



    @Override
    @Transactional
    public void delete(UUID id) {
        VoteSubmission s = voteSubmissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));

        voteSubmissionRepository.deleteById(id);

        // ✅ notify agent (deleted)
        notify(
                s.getOrganization().getOrgId(), s.getAgent().getUserId(),
                NotificationType.VOTE,
                "Submission Deleted",
                "Your vote submission for " + s.getPollingCenter().getCenterName() + " was deleted.",
                "vote_submission", s.getSubmissionId(),
                NotificationPriority.LOW, DeliveryMethod.IN_APP
        );
    }

    @Override
    @Transactional(readOnly = true)
    public VoteSubmissionDto get(UUID id) {
        return voteSubmissionRepository.findById(id).map(mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));
    }

    private void notify(UUID orgId, UUID userId,
                        NotificationType type, String title, String message,
                        String relatedTable, UUID relatedId,
                        NotificationPriority priority, DeliveryMethod method) {
        Set<DeliveryMethod> channels = EnumSet.of(method);
        NotificationCreateRequest r = NotificationCreateRequest.builder()
                .orgId(orgId)
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .relatedTable(relatedTable)
                .relatedId(relatedId)
                .priority(priority != null ? priority : NotificationPriority.NORMAL)
                .channels(channels)
                .build();
        notificationService.publish(r);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<VoteSubmissionDto> search(UUID orgId, UUID electionId, UUID centerId, UUID agentId,
                                          VoteStatus status, LocalDateTime from, LocalDateTime to, String q,
                                          Pageable pageable) {
        Specification<VoteSubmission> spec = Specification
                .where(VoteSubmissionSpecs.orgEquals(orgId))
                .and(VoteSubmissionSpecs.electionEquals(electionId))
                .and(VoteSubmissionSpecs.centerEquals(centerId))
                .and(VoteSubmissionSpecs.agentEquals(agentId))
                .and(VoteSubmissionSpecs.statusEquals(status))
                .and(VoteSubmissionSpecs.between(from, to))
                .and(VoteSubmissionSpecs.textSearch(q));

        return voteSubmissionRepository.findAll(spec, pageable).map(mapper::toDTO);
    }


    @Override
    @Transactional(readOnly = true)
    public long countVisibleSubmissions() {
        Object x = em.createNativeQuery("SELECT COUNT(*) FROM public.vote_submission").getSingleResult();
        return ((Number)x).longValue(); // RLS, if enabled, will scope this automatically
    }


    // ---------- helpers ----------
    private static void validateTally(Map<UUID,Integer> votes, int invalid, int blank, int rejected, int spoiled,
                                      int cast, int registered, Integer ballotsIssued) {
        long sumVotes = votes == null ? 0L : votes.values().stream().mapToLong(Integer::longValue).sum();
        validateTallyInternal(sumVotes, invalid, blank, rejected, spoiled, cast, registered, ballotsIssued);
    }



    private static void validateTallyInternal(long sumVotes, int invalid, int blank, int rejected, int spoiled,
                                              int cast, int registered, Integer ballotsIssued) {
        if (cast < 0 || registered < 0) throw new ResponseStatusException(BAD_REQUEST, "Negative counts not allowed");
        if (invalid < 0 || blank < 0 || rejected < 0 || spoiled < 0) throw new ResponseStatusException(BAD_REQUEST, "Negative category counts not allowed");
        long accounted = sumVotes + invalid + blank + rejected + spoiled;
        if (accounted > cast) throw new ResponseStatusException(BAD_REQUEST, "Accounted ballots exceed ballotsCast");
        if (ballotsIssued != null && cast > ballotsIssued) throw new ResponseStatusException(BAD_REQUEST, "ballotsCast exceeds ballotsIssued");
        if (cast > registered) throw new ResponseStatusException(BAD_REQUEST, "ballotsCast exceeds totalRegisteredVoters");
    }


    private static Map<UUID,Integer> readVotes(String json) {
        try {
            var type = new TypeReference<Map<UUID,Integer>>() {};
            return new ObjectMapper().readValue(json, type);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid candidateVotes JSON in stored submission");
        }
    }

    private static String buildSubmissionHash(UUID orgId,
                                              UUID electionId,
                                              UUID centerId,
                                              UUID placeId,
                                              UUID agentId,
                                              Map<UUID, Integer> votesMap,
                                              int cast,
                                              int invalid,
                                              int blank,
                                              int rejected,
                                              int spoiled) {

        String votesJson;
        try {
            Map<String, Integer> sorted = new TreeMap<>();
            if (votesMap != null) {
                votesMap.forEach((uuid, value) -> sorted.put(uuid.toString(), value));
            }
            votesJson = JSON.writeValueAsString(sorted);
        } catch (Exception ex) {
            votesJson = "{}";
        }

        String payload = orgId + "|" +
                electionId + "|" +
                centerId + "|" +
                placeId + "|" +
                agentId + "|" +
                votesJson + "|" +
                cast + "|" +
                invalid + "|" +
                blank + "|" +
                rejected + "|" +
                spoiled;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }


    private static String buildSubmissionPayloadHash(VoteSubmission s) {
        return buildSubmissionHash(
                s.getOrganization().getOrgId(),
                s.getElection().getElectionId(),
                s.getPollingCenter().getCenterId(),
                s.getPollingPlace().getPlaceId(),
                s.getAgent().getUserId(),
                s.getCandidateVotes(),
                s.getBallotsCast(),
                s.getInvalidBallots(),
                s.getBlankBallots(),
                s.getRejectedBallots(),
                s.getSpoiledBallots()
        );
    }


    private UUID toUuid(Object o) {
        if (o == null) return null;
        if (o instanceof UUID) return (UUID) o;
        if (o instanceof String) return UUID.fromString((String) o);
        // handle driver-specific PGObject or other wrappers
        return UUID.fromString(o.toString());
    }


    private static int nz(Integer x){ return x==null?0:x; }


    private void attachFilesToSubmission(Organization org,
                                         VoteSubmission submission,
                                         SystemUser uploadedBy,
                                         List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return;

        // store in file_upload via FileUploadService
        List<FileUploadDto> stored = fileUploadService.saveAllForEntity(
                org, "vote_submission", submission.getSubmissionId(), uploadedBy, files, Map.of("source", "agent_upload")
        );

        // Mirror image uploads into tally_sheet table (dedupe by sha within a submission)
        for (FileUploadDto f : stored) {
            String mime = f.getMimeType();
            if (mime != null && mime.startsWith("image/")) {
                if (f.getSha256() != null && tallySheetRepository.existsBySubmissionAndSha(submission.getSubmissionId(), f.getSha256())) {
                    continue;
                }
                TallySheet t = new TallySheet();
                t.setOrganization(org);
                t.setSubmission(submission);
                t.setImageUrl(f.getFileUrl());
                t.setFileSha256(f.getSha256());
                tallySheetRepository.save(t);
            }

            auditLogService.logTallyUpload(
                    org.getOrgId(),
                    uploadedBy.getUserId(),
                    "TallySheet",
                    "Uploaded tally sheet for submission: " + submission.getSubmissionId()
            );
        }
    }


    /**
     * Sign the provided chainHash using SigningService and persist signature fields on the submission.
     *
     * @param chainHash   hex string to sign
     * @param submission  the VoteSubmission entity to update and save
     * @param orgId       org id for audit logging
     * @param actorUserId actor user id for audit logging (may be null for system)
     */
    private void signAndPersistChainHash(String chainHash, Backend.ElectionVote.entity.VoteSubmission submission, java.util.UUID orgId, java.util.UUID actorUserId) {
        SigningService.SignResult signResult;
        try {
            signResult = signingService.signHex(chainHash);
        } catch (Exception ex) {
            log.error("Failed to sign chain hash for submission {}: {}", submission.getSubmissionId(), ex.getMessage(), ex);

            // Best-effort audit log; do not let audit failure mask the signing failure
            try {
                auditLogService.log(orgId, actorUserId, Backend.ElectionVote.enums.ActivityType.SYSTEM_ERROR, "vote_submission", "Signing failed for submission " + submission.getSubmissionId() + ": " + ex.getMessage());
            } catch (Exception auditEx) {
                log.warn("Audit logging failed after signing error for submission {}: {}", submission.getSubmissionId(), auditEx.getMessage(), auditEx);
            }

            // Propagate a clean 500 so callers/controllers fail fast and no partial unsigned ledger entries are left
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to sign submission");
        }

        // Persist signature info on the submission entity. Adjust field setters if your entity uses different names/types.
        submission.setSubmissionSignature(signResult.signature());

        // signResult.keyId() may be UUID or String depending on your SigningService implementation.
        // Try setting a UUID first; if your entity expects String change accordingly.
        try {
            // If keyId is UUID
            submission.setSubmissionSignerKeyId((java.util.UUID) signResult.keyId());
        } catch (ClassCastException cce) {
            // Fallback: try to coerce to UUID if string representation is a UUID, otherwise save as null or as string field if available
            Object keyIdObj = signResult.keyId();
            if (keyIdObj != null) {
                String keyIdStr = String.valueOf(keyIdObj);
                try {
                    java.util.UUID keyUuid = java.util.UUID.fromString(keyIdStr);
                    submission.setSubmissionSignerKeyId(keyUuid);
                } catch (IllegalArgumentException iae) {
                    // If your entity only supports UUID signer-key id, and the returned keyId is not a UUID,
                    // you may want to store null or handle differently. Here we set null and log.
                    log.warn("Signing service returned non-UUID keyId for submission {}: {}", submission.getSubmissionId(), keyIdStr);
                    submission.setSubmissionSignerKeyId(null);
                }
            } else {
                submission.setSubmissionSignerKeyId(null);
            }
        }

        // Persist the updated submission (caller can also handle save if preferred)
        voteSubmissionRepository.save(submission);
    }





//    @Override
//    @Transactional
//    public VoteSubmissionDto create(VoteSubmissionCreateRequest req, List<MultipartFile> files) {
//        // delegate to new implementation without request (null)
//        return create(req, files, null);
//    }
//
//    @Override
//    @Transactional
//    public VoteSubmissionDto create(VoteSubmissionCreateRequest req) {
//        // delegate to new implementation without request (null)
//        return create(req, (HttpServletRequest) null);
//    }
//
//    // ---------------- new methods that accept HttpServletRequest ----------------
//    @Override
//    @Transactional
//    public VoteSubmissionDto create(VoteSubmissionCreateRequest req, HttpServletRequest request) {
//        return createInternal(req, null, request);
//    }
//
//    @Override
//    @Transactional
//    public VoteSubmissionDto create(VoteSubmissionCreateRequest req, List<MultipartFile> files, HttpServletRequest request) {
//        return createInternal(req, files, request);
//    }
//
//
//    // single internal implementation to avoid duplication
//    private VoteSubmissionDto createInternal(VoteSubmissionCreateRequest req, List<MultipartFile> files, HttpServletRequest request) {
//
//        Organization org = orgRepo.findById(req.getOrgId())
//                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));
//
//        Election e = electionRepo.findById(req.getElectionId())
//                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Election not found"));
//
//        PollingCenter c = centerRepo.findById(req.getCenterId())
//                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling center not found"));
//
//        SystemUser agent = userRepo.findById(req.getAgentId())
//                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Agent not found"));
//
//        PollingPlace p = placeRepo.findById(req.getPlaceId())
//                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling place not found"));
//
//        // --------- Lookups ---------
//
//        // Ensure the place belongs to the specified center
//        if (!p.getPollingCenter().getCenterId().equals(c.getCenterId())) {
//            throw new ResponseStatusException(BAD_REQUEST, "Polling place does not belong to the specified polling center");
//        }
//
//        // --------- Allocation (per election + place) ---------
//        var alloc = placeAllocationRepo
//                .findByElection_ElectionIdAndPollingPlace_PlaceId(e.getElectionId(), p.getPlaceId())
//                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Polling place not allocated for this election"));
//
//        // --------- Validate tally against allocation ---------
//        // re-use existing validateTally method (assumed present). nz() converts null -> 0
//        validateTally(
//                req.getCandidateVotes(),          // <-- getter
//                nz(req.getInvalidBallots()),
//                nz(req.getBlankBallots()),
//                nz(req.getRejectedBallots()),
//                nz(req.getSpoiledBallots()),
//                nz(req.getBallotsCast()),
//                alloc.getRegisteredVoters(),
//                alloc.getBallotsIssued()
//        );
//
//
//        // --------- Build entity from mapper then set server-side fields ---------
//        VoteSubmission s = mapper.toEntity(req, org, e, c, agent);
//        s.setPollingPlace(p);
//
//        // set clientIp / userAgent from the HttpServletRequest (server-side)
//        if (request != null) {
//            s.setClientIp(RequestUtils.getClientIp(request));
//            s.setUserAgent(RequestUtils.getUserAgent(request));
//        } else {
//            // ensure non-null values for audit; optional
//            s.setClientIp(null);
//            s.setUserAgent(null);
//        }
//
//
//        // set gps location if client supplied lat/lon in request DTO
//        if (req.getLatitude() != null && req.getLongitude() != null) {
//            double lat = req.getLatitude();
//            double lon = req.getLongitude();
//            Point gps = geometryFactory.createPoint(new Coordinate(lon, lat)); // lon,lat
//            gps.setSRID(4326);
//            s.setGpsLocation(gps);
//        }
//
//        // idempotency: if client provided idempotencyKey, return existing record if present
//        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
//            voteSubmissionRepository.findByIdempotencyKey(req.getIdempotencyKey()).ifPresent(existing -> {
//                throw new ResponseStatusException(CONFLICT, "Submission with this idempotency key already exists: " + existing.getSubmissionId());
//            });
//            s.setIdempotencyKey(req.getIdempotencyKey());
//        }
//
//        // include place in the hash so same content at a different place is NOT a duplicate
//        s.setSubmissionHash(buildSubmissionHash(
//                org.getOrgId(),
//                e.getElectionId(),
//                c.getCenterId(),
//                p.getPlaceId(),
//                agent.getUserId(),
//                s.getCandidateVotes(),
//                s.getBallotsCast(),
//                s.getInvalidBallots(),
//                s.getBlankBallots(),
//                s.getRejectedBallots(),
//                s.getSpoiledBallots()
//        ));
//
//        if (voteSubmissionRepository.existsBySubmissionHash(s.getSubmissionHash())) {
//            throw new ResponseStatusException(CONFLICT, "Duplicate submission (same content).");
//        }
//
//        s.setVersion(1);
//        VoteSubmission saved = voteSubmissionRepository.save(s);
//
//        // --------- Attach files (tally sheets) ---------
//        if (files != null && !files.isEmpty()) {
//            attachFilesToSubmission(org, saved, agent, files);
//        }
//
//        // --------- Audit log ---------
//        auditLogService.logSubmissionCreate(
//                org.getOrgId(),
//                agent.getUserId(),
//                "VoteSubmission",
//                "Created submission: " + saved.getSubmissionId() +
//                        " at " + c.getCenterName() +
//                        " / place: " + p.getCode()
//        );
//
//        // --------- Notify agent ---------
//        String placeDisplay = (p.getLabel() != null && !p.getLabel().isBlank())
//                ? p.getLabel()
//                : "Place " + p.getPlaceNumber();
//
//        notify(
//                org.getOrgId(), agent.getUserId(),
//                NotificationType.VOTE,
//                "Submission Received",
//                "Your vote submission for " + c.getCenterName() + " - " + placeDisplay + " was received.",
//                "vote_submission", saved.getSubmissionId(),
//                NotificationPriority.NORMAL, DeliveryMethod.IN_APP
//        );
//
//        return mapper.toDTO(saved);
//    }
//
//

    //    @Override
//    @Transactional
//    public VoteSubmissionDto verify(UUID id, VoteSubmissionVerifyRequest req) {
//
//        VoteSubmission s = voteSubmissionRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));
//        SystemUser verifier = userRepo.findById(req.getVerifierUserId())
//                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Verifier not found"));
//
//        if (s.getStatus() != VoteStatus.PENDING) {
//            throw new ResponseStatusException(BAD_REQUEST, "Submission already processed");
//        }
//
//        // decide new status
//        boolean accept = Boolean.TRUE.equals(req.getAccept());
//        s.setStatus(accept ? VoteStatus.VERIFIED : VoteStatus.REJECTED);
//        s.setVerifiedBy(verifier);
//        s.setDateVerified(LocalDateTime.now());
//
//        // append review comment to existing comments
//        if (req.getComment() != null && !req.getComment().isBlank()) {
//            String prefix = (s.getComments() == null ? "" : s.getComments() + "\n");
//            s.setComments(prefix + "[review] " + req.getComment());
//        }
//        try {
//            VoteSubmission saved = voteSubmissionRepository.save(s);
//
//            UUID orgId = saved.getOrganization().getOrgId();
//            UUID electionId = saved.getElection().getElectionId();
//
//            if (saved.getStatus() == VoteStatus.VERIFIED) {
//                // recompute aggregated tallies for this org + election
//                voteTallyService.recomputeForElection(
//                        orgId,
//                        electionId,
//                        verifier.getUserId()
//                );
//                // 2) notify agent
//                notify(
//                        saved.getOrganization().getOrgId(), saved.getAgent().getUserId(),
//                        NotificationType.VOTE,
//                        "Submission Verified",
//                        "Your submission at " + saved.getPollingCenter().getCenterName() + " was verified.",
//                        "vote_submission", saved.getSubmissionId(),
//                        NotificationPriority.NORMAL, DeliveryMethod.IN_APP
//                );
//
//                // 3) audit log: VERIFIED
//                auditLogService.logSubmissionVerify(
//                        saved.getOrganization().getOrgId(),
//                        verifier.getUserId(),
//                        "VoteSubmission",
//                        "Verified submission: " + saved.getSubmissionId()
//                );
//
//            } else {
//
//                voteTallyService.recomputeForElection(orgId, electionId, verifier.getUserId());
//
//                // 2) notify agent
//                String message = "Your submission at " + saved.getPollingCenter().getCenterName() + " was rejected.";
//                if (req.getComment() != null && !req.getComment().isBlank()) {
//                    message += " Reason: " + req.getComment();
//                }
//
//                notify(
//                        saved.getOrganization().getOrgId(), saved.getAgent().getUserId(),
//                        NotificationType.VOTE,
//                        "Submission Rejected",
//                        message,
//                        "vote_submission", saved.getSubmissionId(),
//                        NotificationPriority.NORMAL, DeliveryMethod.IN_APP
//                );
//
//                // 3) audit log: REJECTED
//                auditLogService.logSubmissionReject(
//                        saved.getOrganization().getOrgId(),
//                        verifier.getUserId(),
//                        "VoteSubmission",
//                        "Rejected submission: " + saved.getSubmissionId() +
//                                (req.getComment() != null && !req.getComment().isBlank()
//                                        ? " Reason: " + req.getComment()
//                                        : "")
//                );
//            }
//
//            return mapper.toDTO(saved);
//
//        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
//            // Adjust this message if your unique index is now org + election + place instead of center
//            throw new ResponseStatusException(
//                    CONFLICT,
//                    "A verified submission already exists for this organization, election, and polling location"
//            );
//        }
//    }


//    // ----------------- UPDATE (no files) -----------------
//    @Override
//    public VoteSubmissionDto update(UUID id, VoteSubmissionUpdateRequest req) {
//        return update(id, req, null);
//    }
//
//



//    // ----------------- UPDATE (with files) -----------------
//    @Override
//    @Transactional
//    public VoteSubmissionDto update(UUID id, VoteSubmissionUpdateRequest req, List<MultipartFile> files) {
//        VoteSubmission s = voteSubmissionRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));
//
//        if (s.getStatus() != VoteStatus.PENDING) {
//            throw new ResponseStatusException(BAD_REQUEST, "Only PENDING submissions can be updated");
//        }
//        // ---- Merge current + incoming values ----
//        Map<UUID, Integer> mergedVotes =
//                (req.getCandidateVotes() != null)
//                        ? req.getCandidateVotes()
//                        : (s.getCandidateVotes() != null ? s.getCandidateVotes() : Collections.emptyMap());
//
//        int cast    = (req.getBallotsCast()    != null) ? req.getBallotsCast()    : s.getBallotsCast();
//        int invalid = (req.getInvalidBallots() != null) ? req.getInvalidBallots() : s.getInvalidBallots();
//        int blank   = (req.getBlankBallots()   != null) ? req.getBlankBallots()   : s.getBlankBallots();
//        int rej     = (req.getRejectedBallots()!= null) ? req.getRejectedBallots(): s.getRejectedBallots();
//        int spo     = (req.getSpoiledBallots() != null) ? req.getSpoiledBallots() : s.getSpoiledBallots();
//
//        // ---- Allocation is now per PLACE (not center) ----
//        PollingPlace place = s.getPollingPlace();
//        if (place == null) {
//            throw new ResponseStatusException(BAD_REQUEST, "Submission is missing polling place reference");
//        }
//
//        var alloc = placeAllocationRepo
//                .findByElection_ElectionIdAndPollingPlace_PlaceId(
//                        s.getElection().getElectionId(),
//                        place.getPlaceId()
//                )
//                .orElseThrow(() ->
//                        new ResponseStatusException(BAD_REQUEST, "Polling place not allocated for this election"));
//
//        validateTally(
//                mergedVotes,
//                invalid,
//                blank,
//                rej,
//                spo,
//                cast,
//                alloc.getRegisteredVoters(),
//                alloc.getBallotsIssued()
//        );
//
//        // ---- Apply updates ----
//        mapper.apply(req, s);
//        s.setVersion(s.getVersion() + 1);
//
//        // ---- Rebuild hash: now includes placeId (11 args) ----
//        s.setSubmissionHash(buildSubmissionHash(
//                s.getOrganization().getOrgId(),
//                s.getElection().getElectionId(),
//                s.getPollingCenter().getCenterId(),
//                s.getPollingPlace().getPlaceId(),
//                s.getAgent().getUserId(),
//                s.getCandidateVotes(),
//                s.getBallotsCast(),
//                s.getInvalidBallots(),
//                s.getBlankBallots(),
//                s.getRejectedBallots(),
//                s.getSpoiledBallots()
//        ));
//
//        VoteSubmission saved = voteSubmissionRepository.save(s);
//
//        // ✅ attach any appended files
//        if (files != null && !files.isEmpty()) {
//            attachFilesToSubmission(saved.getOrganization(), saved, saved.getAgent(), files);
//        }
//
//        // Audit Log Activity
//        auditLogService.logSubmissionUpdate(
//                s.getOrganization().getOrgId(),
//                s.getAgent().getUserId(),
//                "VoteSubmission",
//                "Updated submission: " + s.getSubmissionId()
//        );
//
//        // ✅ notify agent (submission updated)
//        notify(
//                saved.getOrganization().getOrgId(), saved.getAgent().getUserId(),
//                NotificationType.VOTE,
//                "Submission Updated",
//                "Your vote submission for " + saved.getPollingCenter().getCenterName() + " was updated.",
//                "vote_submission", saved.getSubmissionId(),
//                NotificationPriority.LOW, DeliveryMethod.IN_APP
//        );
//
//        return mapper.toDTO(saved);
//    }






//    // ---------- small helper to keep call sites clean ----------
//    private void notify(UUID orgId, UUID userId,
//                        NotificationType type, String title, String message,
//                        String relatedTable, UUID relatedId,
//                        NotificationPriority priority,
//                        Set<DeliveryMethod> channels,
//                        String idempotencyKey) {
//        NotificationCreateRequest r = NotificationCreateRequest.builder()
//                .orgId(orgId)
//                .userId(userId)
//                .type(type)
//                .title(title)
//                .message(message)
//                .relatedTable(relatedTable)
//                .relatedId(relatedId)
//                .priority(priority != null ? priority : NotificationPriority.NORMAL)
//                .channels((channels == null || channels.isEmpty())
//                        ? EnumSet.of(DeliveryMethod.IN_APP)
//                        : EnumSet.copyOf(channels))
//                .idempotencyKey(idempotencyKey)
//                .build();
//        notificationService.publish(r);
//    }
//
//    private void notify(UUID orgId, UUID userId,
//                        NotificationType type, String title, String message,
//                        String relatedTable, UUID relatedId,
//                        NotificationPriority priority, DeliveryMethod method) {
//        notify(orgId, userId, type, title, message, relatedTable, relatedId,
//                priority, EnumSet.of(method), null);
//    }



    //    // ---------- helpers ----------
//    private static void validateTally(Map<UUID,Integer> votes, int invalid, int blank, int rejected, int spoiled,
//                                      int cast, int registered, Integer ballotsIssued) {
//        long sumVotes = votes == null ? 0L : votes.values().stream().mapToLong(Integer::longValue).sum();
//        validateTallyInternal(sumVotes, invalid, blank, rejected, spoiled, cast, registered, ballotsIssued);
//    }



    //    private static void validateTallyInternal(long sumVotes, int invalid, int blank, int rejected, int spoiled,
//                                              int cast, int registered, Integer ballotsIssued) {
//        if (cast < 0 || registered < 0) throw new ResponseStatusException(BAD_REQUEST, "Negative counts not allowed");
//        if (invalid < 0 || blank < 0 || rejected < 0 || spoiled < 0) throw new ResponseStatusException(BAD_REQUEST, "Negative category counts not allowed");
//        long accounted = sumVotes + invalid + blank + rejected + spoiled;
//        if (accounted > cast) throw new ResponseStatusException(BAD_REQUEST, "Accounted ballots exceed ballotsCast");
//        if (ballotsIssued != null && cast > ballotsIssued) throw new ResponseStatusException(BAD_REQUEST, "ballotsCast exceeds ballotsIssued");
//        if (cast > registered) throw new ResponseStatusException(BAD_REQUEST, "ballotsCast exceeds totalRegisteredVoters");
//    }




//    private static String buildSubmissionHash(UUID orgId,
//                                              UUID electionId,
//                                              UUID centerId,
//                                              UUID placeId,
//                                              UUID agentId,
//                                              Map<UUID, Integer> votesMap,
//                                              int cast,
//                                              int invalid,
//                                              int blank,
//                                              int rejected,
//                                              int spoiled) {
//
//        // ---- Convert Map<UUID,Integer> → sorted JSON string ----
//        String votesJson;
//        try {
//            // Convert UUID keys to Strings + sort for stable hashing
//            Map<String, Integer> sorted = new TreeMap<>();
//            if (votesMap != null) {
//                votesMap.forEach((uuid, value) -> sorted.put(uuid.toString(), value));
//            }
//
//            ObjectMapper mapper = new ObjectMapper();
//            votesJson = mapper.writeValueAsString(sorted);   // e.g. {"uuid1":100,"uuid2":200}
//        } catch (Exception ex) {
//            votesJson = "{}"; // safe fallback
//        }
//
//        // ---- Build raw payload identical to original structure ----
//        String payload = orgId + "|" +
//                electionId + "|" +
//                centerId + "|" +
//                placeId + "|" +
//                agentId + "|" +
//                votesJson + "|" +
//                cast + "|" +
//                invalid + "|" +
//                blank + "|" +
//                rejected + "|" +
//                spoiled;
//
//        // ---- SHA-256 hash (same output format as before) ----
//        try {
//            MessageDigest md = MessageDigest.getInstance("SHA-256");
//            return HexFormat.of().formatHex(md.digest(payload.getBytes(StandardCharsets.UTF_8)));
//        } catch (NoSuchAlgorithmException ex) {
//            throw new IllegalStateException("SHA-256 unavailable", ex);
//        }
//    }


//    // ----------------- FILE ATTACHMENT HELPER -----------------
//    private void attachFilesToSubmission(Organization org,
//                                         VoteSubmission submission,
//                                         SystemUser uploadedBy,
//                                         List<MultipartFile> files) {
//        if (files == null || files.isEmpty()) return;
//
//        // store in file_upload
//        List<FileUploadDto> stored = fileUploadService.saveAllForEntity(
//                org, "vote_submission", submission.getSubmissionId(), uploadedBy, files, Map.of("source", "agent_upload")
//        );
//
//        // for images, mirror into tally_sheet table
//        for (FileUploadDto f : stored) {
//            String mime = f.getMimeType();
//            if (mime != null && mime.startsWith("image/")) {
//                // optional de-dup by sha per submission
//                if (f.getSha256() != null && tallySheetRepository.existsBySubmissionAndSha(submission.getSubmissionId(), f.getSha256())) {
//                    continue;
//                }
//                TallySheet t = new TallySheet();
//                t.setOrganization(org);
//                t.setSubmission(submission);
//                t.setImageUrl(f.getFileUrl());
//                t.setFileSha256(f.getSha256());
//                // dateUploaded via @PrePersist
//                tallySheetRepository.save(t);
//            }
//
//            // Audit Log Activity
//            auditLogService.logTallyUpload(
//                    org.getOrgId(),
//                    uploadedBy.getUserId(),
//                    "TallySheet",
//                    "Uploaded tally sheet for submission: " + submission
//            );
//        }
//    }


}
