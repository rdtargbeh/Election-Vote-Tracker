package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.*;
import Backend.ElectionVote.entity.*;
import Backend.ElectionVote.enums.DeliveryMethod;
import Backend.ElectionVote.enums.NotificationPriority;
import Backend.ElectionVote.enums.NotificationType;
import Backend.ElectionVote.enums.VoteStatus;
import Backend.ElectionVote.mapper.VoteSubmissionMapper;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.service.*;
import Backend.ElectionVote.utility.VoteSubmissionSpecs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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


@Service
@RequiredArgsConstructor
public class VoteSubmissionServiceImplementation implements VoteSubmissionService {

    private final EntityManager em;

    private final VoteSubmissionRepository voteSubmissionRepository;
    private final OrganizationRepository orgRepo;
    private final ElectionRepository electionRepo;
    private final SystemUserRepository userRepo;
    private final VoteDetailRepository voteDetailRepository;
    private final VoteDetailService voteDetailService;
    private final NotificationService notificationService;

    private final PollingCenterRepository centerRepo;
    private final PollingPlaceRepository placeRepo;
    private final PollingPlaceAllocationRepository placeAllocationRepo;
    private final PollingCenterAllocationRepository allocationRepo;

    private final FileUploadService fileUploadService;
    private final TallySheetRepository tallySheetRepository;
    private final AuditLogService auditLogService;

    private final VoteSubmissionMapper mapper;


    @Override
    public VoteSubmissionDto create(VoteSubmissionCreateRequest req) {
        return create(req, null);
    }

    @Override
    @Transactional
    public VoteSubmissionDto create(VoteSubmissionCreateRequest req, List<MultipartFile> files) {
        // --------- Lookups ---------
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

        // Ensure the place belongs to the specified center
        if (!p.getPollingCenter().getCenterId().equals(c.getCenterId())) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Polling place does not belong to the specified polling center"
            );
        }

        // --------- Allocation (per election + place) ---------
        var alloc = placeAllocationRepo
                .findByElection_ElectionIdAndPollingPlace_PlaceId(e.getElectionId(), p.getPlaceId())
                .orElseThrow(() ->
                        new ResponseStatusException(BAD_REQUEST, "Polling place not allocated for this election"));

        // --------- Validate tally against allocation ---------
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

        // --------- Build entity ---------
        VoteSubmission s = mapper.toEntity(req, org, e, c, agent);
        s.setPollingPlace(p);

        // include place in the hash so same content at a different place is NOT a duplicate
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

        s.setVersion(1);
        VoteSubmission saved = voteSubmissionRepository.save(s);

        // --------- Attach files (tally sheets) ---------
        if (files != null && !files.isEmpty()) {
            attachFilesToSubmission(org, saved, agent, files);
        }
        // --------- Audit log ---------
        auditLogService.logSubmissionCreate(
                org.getOrgId(),
                agent.getUserId(),
                "VoteSubmission",
                "Created submission: " + saved.getSubmissionId() +
                        " at " + c.getCenterName() +
                        " / place: " + p.getCode()
        );
        // --------- Notify agent ---------
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


    // ----------------- UPDATE (no files) -----------------
    @Override
    public VoteSubmissionDto update(UUID id, VoteSubmissionUpdateRequest req) {
        return update(id, req, null);
    }


    // ----------------- UPDATE (with files) -----------------
    @Override
    @Transactional
    public VoteSubmissionDto update(UUID id, VoteSubmissionUpdateRequest req, List<MultipartFile> files) {
        VoteSubmission s = voteSubmissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));

        if (s.getStatus() != VoteStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Only PENDING submissions can be updated");
        }

        // ---- Merge current + incoming values ----
        Map<UUID, Integer> mergedVotes =
                (req.getCandidateVotes() != null)
                        ? req.getCandidateVotes()
                        : (s.getCandidateVotes() != null ? s.getCandidateVotes() : Collections.emptyMap());

        int cast    = (req.getBallotsCast()    != null) ? req.getBallotsCast()    : s.getBallotsCast();
        int invalid = (req.getInvalidBallots() != null) ? req.getInvalidBallots() : s.getInvalidBallots();
        int blank   = (req.getBlankBallots()   != null) ? req.getBlankBallots()   : s.getBlankBallots();
        int rej     = (req.getRejectedBallots()!= null) ? req.getRejectedBallots(): s.getRejectedBallots();
        int spo     = (req.getSpoiledBallots() != null) ? req.getSpoiledBallots() : s.getSpoiledBallots();

        // ---- Allocation is now per PLACE (not center) ----
        PollingPlace place = s.getPollingPlace();
        if (place == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Submission is missing polling place reference");
        }

        var alloc = placeAllocationRepo
                .findByElection_ElectionIdAndPollingPlace_PlaceId(
                        s.getElection().getElectionId(),
                        place.getPlaceId()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(BAD_REQUEST, "Polling place not allocated for this election"));

        validateTally(
                mergedVotes,
                invalid,
                blank,
                rej,
                spo,
                cast,
                alloc.getRegisteredVoters(),
                alloc.getBallotsIssued()
        );

        // ---- Apply updates ----
        mapper.apply(req, s);
        s.setVersion(s.getVersion() + 1);

        // ---- Rebuild hash: now includes placeId (11 args) ----
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

        // ✅ attach any appended files
        if (files != null && !files.isEmpty()) {
            attachFilesToSubmission(saved.getOrganization(), saved, saved.getAgent(), files);
        }

        // Audit Log Activity
        auditLogService.logSubmissionUpdate(
                s.getOrganization().getOrgId(),
                s.getAgent().getUserId(),
                "VoteSubmission",
                "Updated submission: " + s.getSubmissionId()
        );

        // ✅ notify agent (submission updated)
        notify(
                saved.getOrganization().getOrgId(), saved.getAgent().getUserId(),
                NotificationType.VOTE,
                "Submission Updated",
                "Your vote submission for " + saved.getPollingCenter().getCenterName() + " was updated.",
                "vote_submission", saved.getSubmissionId(),
                NotificationPriority.LOW, DeliveryMethod.IN_APP
        );

        return mapper.toDTO(saved);
    }


    // ---------- small helper to keep call sites clean ----------
    private void notify(UUID orgId, UUID userId,
                        NotificationType type, String title, String message,
                        String relatedTable, UUID relatedId,
                        NotificationPriority priority,
                        Set<DeliveryMethod> channels,
                        String idempotencyKey) {
        NotificationCreateRequest r = NotificationCreateRequest.builder()
                .orgId(orgId)
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .relatedTable(relatedTable)
                .relatedId(relatedId)
                .priority(priority != null ? priority : NotificationPriority.NORMAL)
                .channels((channels == null || channels.isEmpty())
                        ? EnumSet.of(DeliveryMethod.IN_APP)
                        : EnumSet.copyOf(channels))
                .idempotencyKey(idempotencyKey)
                .build();
        notificationService.publish(r);
    }

    private void notify(UUID orgId, UUID userId,
                        NotificationType type, String title, String message,
                        String relatedTable, UUID relatedId,
                        NotificationPriority priority, DeliveryMethod method) {
        notify(orgId, userId, type, title, message, relatedTable, relatedId,
                priority, EnumSet.of(method), null);
    }


    // ----------------- VERIFY (unchanged notifications) -----------------
    @Override
    public VoteSubmissionDto verify(UUID id, VoteSubmissionVerifyRequest req) {
        VoteSubmission s = voteSubmissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));

        SystemUser verifier = userRepo.findById(req.getVerifierUserId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Verifier not found"));

        if (s.getStatus() != VoteStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Submission already processed");
        }

        s.setStatus(req.getAccept() ? VoteStatus.VERIFIED : VoteStatus.REJECTED);
        s.setVerifiedBy(verifier);
        s.setDateVerified(LocalDateTime.now());

        if (req.getComment() != null && !req.getComment().isBlank()) {
            String prefix = (s.getComments() == null ? "" : s.getComments() + "\n");
            s.setComments(prefix + "[review] " + req.getComment());
        }

        try {
            VoteSubmission saved = voteSubmissionRepository.save(s);

            if (saved.getStatus() == VoteStatus.VERIFIED) {
                voteDetailService.resyncFromSubmission(saved.getSubmissionId());
                notify(
                        saved.getOrganization().getOrgId(), saved.getAgent().getUserId(),
                        NotificationType.VOTE,
                        "Submission Verified",
                        "Your submission at " + saved.getPollingCenter().getCenterName() + " was verified.",
                        "vote_submission", saved.getSubmissionId(),
                        NotificationPriority.NORMAL, DeliveryMethod.IN_APP
                );

                // Audit Log Activity
                auditLogService.logSubmissionVerify(
                        saved.getOrganization().getOrgId(),
                        verifier.getUserId(),
                        "VoteSubmission",
                        "Verified submission: " + saved.getSubmissionId()
                );

            } else {
                voteDetailRepository.deleteBySubmissionId(saved.getSubmissionId());
                notify(
                        saved.getOrganization().getOrgId(), saved.getAgent().getUserId(),
                        NotificationType.VOTE,
                        "Submission Rejected",
                        "Your submission at " + saved.getPollingCenter().getCenterName() + " was rejected."
                                + (req.getComment()!=null && !req.getComment().isBlank()? " Reason: "+req.getComment() : ""),
                        "vote_submission", saved.getSubmissionId(),
                        NotificationPriority.NORMAL, DeliveryMethod.IN_APP
                );
            }

            // Audit Log Activity
            auditLogService.logSubmissionReject(
                    saved.getOrganization().getOrgId(),
                    verifier.getUserId(),
                    "VoteSubmission",
                    "Rejected submission: " + saved.getSubmissionId() +
                            (req.getComment()!=null && !req.getComment().isBlank()? " Reason: "+req.getComment() : "")
            );

            return mapper.toDTO(saved);

        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT,
                    "A verified submission already exists for this organization, election, and center");
        }
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

        // ---- Convert Map<UUID,Integer> → sorted JSON string ----
        String votesJson;
        try {
            // Convert UUID keys to Strings + sort for stable hashing
            Map<String, Integer> sorted = new TreeMap<>();
            if (votesMap != null) {
                votesMap.forEach((uuid, value) -> sorted.put(uuid.toString(), value));
            }

            ObjectMapper mapper = new ObjectMapper();
            votesJson = mapper.writeValueAsString(sorted);   // e.g. {"uuid1":100,"uuid2":200}
        } catch (Exception ex) {
            votesJson = "{}"; // safe fallback
        }

        // ---- Build raw payload identical to original structure ----
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

        // ---- SHA-256 hash (same output format as before) ----
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }




    private static int nz(Integer x){ return x==null?0:x; }


    // ----------------- FILE ATTACHMENT HELPER -----------------
    private void attachFilesToSubmission(Organization org,
                                         VoteSubmission submission,
                                         SystemUser uploadedBy,
                                         List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return;

        // store in file_upload
        List<FileUploadDto> stored = fileUploadService.saveAllForEntity(
                org, "vote_submission", submission.getSubmissionId(), uploadedBy, files, Map.of("source", "agent_upload")
        );

        // for images, mirror into tally_sheet table
        for (FileUploadDto f : stored) {
            String mime = f.getMimeType();
            if (mime != null && mime.startsWith("image/")) {
                // optional de-dup by sha per submission
                if (f.getSha256() != null && tallySheetRepository.existsBySubmissionAndSha(submission.getSubmissionId(), f.getSha256())) {
                    continue;
                }
                TallySheet t = new TallySheet();
                t.setOrganization(org);
                t.setSubmission(submission);
                t.setImageUrl(f.getFileUrl());
                t.setFileSha256(f.getSha256());
                // dateUploaded via @PrePersist
                tallySheetRepository.save(t);
            }

            // Audit Log Activity
            auditLogService.logTallyUpload(
                    org.getOrgId(),
                    uploadedBy.getUserId(),
                    "TallySheet",
                    "Uploaded tally sheet for submission: " + submission
            );
        }
    }
}
