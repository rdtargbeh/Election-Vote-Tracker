package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.VoteSubmissionRankingDto;
import Backend.ElectionVote.entity.ContestOption;
import Backend.ElectionVote.entity.VoteSubmission;
import Backend.ElectionVote.entity.VoteSubmissionRanking;
import Backend.ElectionVote.repository.ContestOptionRepository;
import Backend.ElectionVote.repository.VoteSubmissionRankingRepository;
import Backend.ElectionVote.repository.VoteSubmissionRepository;
import Backend.ElectionVote.service.AuditLogService;
import Backend.ElectionVote.service.VoteSubmissionRankingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Service that handles persistence and validation for submission_vote_ranking.
 *
 * Validations and rules implemented:
 * - ranking must be a JSON array (each element a UUID string).
 * - Each UUID in ranking must correspond to an option_id in contest_option for the contest.
 * - Idempotent create/update: if a ranking exists for submission+contest (via explicit lookup),
 *   this method creates a new row or updates existing by svrId if provided.
 *
 * Assumptions:
 * - VoteSubmission entity exists and (optionally) may contain ranking JSON if you wish to backfill.
 * - If VoteSubmission doesn't store ranking JSON, backfill will skip that submission.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VoteSubmissionRankingServiceImplementation implements VoteSubmissionRankingService {

    private static final Logger log = LoggerFactory.getLogger(VoteSubmissionRankingServiceImplementation.class);

    private final VoteSubmissionRankingRepository svrRepo;
    private final ContestOptionRepository optionRepo;
    private final VoteSubmissionRepository voteSubmissionRepository;
    private final AuditLogService auditLogService;

    @Override
    public VoteSubmissionRankingDto createOrUpdateRanking(VoteSubmissionRankingDto dto) {
        if (dto == null) throw new ResponseStatusException(BAD_REQUEST, "Missing ranking DTO");
        if (dto.getSubmissionId() == null) throw new ResponseStatusException(BAD_REQUEST, "submissionId required");
        if (dto.getContestId() == null) throw new ResponseStatusException(BAD_REQUEST, "contestId required");
        if (dto.getRanking() == null) throw new ResponseStatusException(BAD_REQUEST, "ranking JSON array required");
        if (!dto.getRanking().isArray()) throw new ResponseStatusException(BAD_REQUEST, "ranking must be a JSON array");

        ArrayNode arr = (ArrayNode) dto.getRanking();
        if (arr.size() == 0) throw new ResponseStatusException(BAD_REQUEST, "ranking array must not be empty");

        // Validate each entry is a UUID and exists in contest_option for this contest
        Set<UUID> optionIdsAllowed = optionRepo.findByContestIdOrderByOptionOrderAsc(dto.getContestId())
                .stream().map(ContestOption::getOptionId).collect(Collectors.toSet());

        List<UUID> parsed = new ArrayList<>();
        for (JsonNode n : arr) {
            if (!n.isTextual()) throw new ResponseStatusException(BAD_REQUEST, "each ranking element must be a UUID string");
            String s = n.asText();
            UUID optId;
            try {
                optId = UUID.fromString(s);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(BAD_REQUEST, "invalid UUID in ranking: " + s);
            }
            if (!optionIdsAllowed.contains(optId)) {
                throw new ResponseStatusException(BAD_REQUEST, "ranking contains option that does not belong to contest: " + optId);
            }
            parsed.add(optId);
        }

        // Ensure submission exists
        VoteSubmission vs = voteSubmissionRepository.findById(dto.getSubmissionId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));

        // Upsert: find existing record for submission+contest, update it; otherwise create new
        Optional<VoteSubmissionRanking> existingOpt = svrRepo.findBySubmissionIdAndContestId(dto.getSubmissionId(), dto.getContestId());
        VoteSubmissionRanking entity;
        boolean created = false;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
        } else {
            entity = new VoteSubmissionRanking();
            entity.setSvrId(UUID.randomUUID());
            created = true;
        }

        entity.setSubmissionId(dto.getSubmissionId());
        entity.setContestId(dto.getContestId());
        entity.setRanking(dto.getRanking());

        VoteSubmissionRanking saved = svrRepo.save(entity);

        // Audit log (best-effort)
        try {
            if (created) {
                auditLogService.logCreate(null, null, "submission_vote_ranking",
                        "Created ranking svr=" + saved.getSvrId() + " submission=" + saved.getSubmissionId() +
                                " contest=" + saved.getContestId());
            } else {
                auditLogService.logUpdate(null, null, "submission_vote_ranking",
                        "Updated ranking svr=" + saved.getSvrId() + " submission=" + saved.getSubmissionId() +
                                " contest=" + saved.getContestId());
            }
        } catch (Exception ex) {
            log.debug("Failed to write audit_log for submission_vote_ranking: {}", ex.getMessage());
        }

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VoteSubmissionRankingDto getById(UUID svrId) {
        return svrRepo.findById(svrId).map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ranking not found: " + svrId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoteSubmissionRankingDto> getBySubmission(UUID submissionId) {
        return svrRepo.findBySubmissionId(submissionId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoteSubmissionRankingDto> getByContest(UUID contestId) {
        return svrRepo.findByContestId(contestId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID svrId) {
        if (!svrRepo.existsById(svrId)) {
            throw new ResponseStatusException(NOT_FOUND, "Ranking not found: " + svrId);
        }
        svrRepo.deleteById(svrId);
        try {
            auditLogService.logDelete(null, null, "submission_vote_ranking", "Deleted ranking " + svrId);
        } catch (Exception ex) {
            log.debug("Failed to write audit_log for deletion of submission_vote_ranking: {}", ex.getMessage());
        }
    }

    @Override
    public int backfillFromVerifiedSubmissionsForElection(UUID electionId) {
        List<VoteSubmission> subs = voteSubmissionRepository.findByElection_ElectionIdAndStatusAndDateDeletedIsNull(electionId, "VERIFIED");
        int processed = 0;
        for (VoteSubmission vs : subs) {
            try {
                // Extract ranking and contestId in a robust way
                ExtractedRanking er = extractRankingFromVoteSubmission(vs);
                if (er == null) {
                    // nothing to backfill for this submission
                    continue;
                }

                VoteSubmissionRankingDto dto = new VoteSubmissionRankingDto();
                dto.setSubmissionId(vs.getSubmissionId());
                dto.setContestId(er.contestId);
                dto.setRanking(er.ranking);

                createOrUpdateRanking(dto);
                processed++;
            } catch (ResponseStatusException ex) {
                // validation error for this submission; skip and continue (best-effort)
                log.debug("Skipping submission {} during backfill: {}", vs.getSubmissionId(), ex.getReason());
            } catch (Exception ex) {
                // unexpected error — skip this submission but log
                log.warn("Unexpected error backfilling submission {}: {}", vs.getSubmissionId(), ex.getMessage());
            }
        }
        // Audit the backfill summary
        try {
            auditLogService.logUpdate(null, null, "submission_vote_ranking",
                    "Backfilled rankings for election=" + electionId + " processed=" + processed);
        } catch (Exception ex) {
            log.debug("Failed to audit backfill operation: {}", ex.getMessage());
        }
        return processed;
    }

    /**
     * Attempt to extract ranking JSON array and contestId from a VoteSubmission using deterministic methods
     * and safe reflection fallback. Returns null if no ranking present.
     */
    private ExtractedRanking extractRankingFromVoteSubmission(VoteSubmission vs) {
        JsonNode ranking = null;
        UUID contestId = null;

        // 1) Try direct getters (common method names)
        try {
            // getRanking()
            Method mRanking = VoteSubmission.class.getMethod("getRanking");
            Object r = mRanking.invoke(vs);
            if (r instanceof JsonNode) ranking = (JsonNode) r;
        } catch (NoSuchMethodException ignored) {
        } catch (Exception ex) {
            log.debug("Direct getRanking() invocation failed: {}", ex.getMessage());
        }

        try {
            // getContestId()
            Method mContest = VoteSubmission.class.getMethod("getContestId");
            Object c = mContest.invoke(vs);
            if (c instanceof UUID) contestId = (UUID) c;
        } catch (NoSuchMethodException ignored) {
        } catch (Exception ex) {
            log.debug("Direct getContestId() invocation failed: {}", ex.getMessage());
        }

        // 2) Try alternate common names
        if (ranking == null) {
            try {
                Method m = VoteSubmission.class.getMethod("getRankingJson");
                Object r = m.invoke(vs);
                if (r instanceof JsonNode) ranking = (JsonNode) r;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ex) {
                log.debug("getRankingJson() invocation failed: {}", ex.getMessage());
            }
        }
        if (contestId == null) {
            try {
                Method m = VoteSubmission.class.getMethod("getContest");
                Object c = m.invoke(vs);
                if (c instanceof UUID) contestId = (UUID) c;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ex) {
                log.debug("getContest() invocation failed: {}", ex.getMessage());
            }
        }

        // 3) Reflection fallback: inspect any getters that look like ranking/contest fields
        if (ranking == null) {
            // try to find any method that returns JsonNode and has "ranking" in name
            for (Method m : VoteSubmission.class.getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().toLowerCase().contains("ranking") && JsonNode.class.isAssignableFrom(m.getReturnType())) {
                    try {
                        Object r = m.invoke(vs);
                        if (r instanceof JsonNode) { ranking = (JsonNode) r; break; }
                    } catch (Exception ex) { /* ignore */ }
                }
            }
        }
        if (contestId == null) {
            // try to find any method that returns UUID and has "contest" in name
            for (Method m : VoteSubmission.class.getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().toLowerCase().contains("contest") && UUID.class.isAssignableFrom(m.getReturnType())) {
                    try {
                        Object c = m.invoke(vs);
                        if (c instanceof UUID) { contestId = (UUID) c; break; }
                    } catch (Exception ex) { /* ignore */ }
                }
            }
        }

        // If still no ranking or contestId, nothing to backfill
        if (ranking == null || !ranking.isArray() || ranking.size() == 0 || contestId == null) {
            return null;
        }

        // Return extracted pair
        return new ExtractedRanking(ranking, contestId);
    }

    private static class ExtractedRanking {
        final JsonNode ranking;
        final UUID contestId;
        ExtractedRanking(JsonNode ranking, UUID contestId) {
            this.ranking = ranking;
            this.contestId = contestId;
        }
    }

    private VoteSubmissionRankingDto toDto(VoteSubmissionRanking s) {
        if (s == null) return null;
        VoteSubmissionRankingDto d = new VoteSubmissionRankingDto();
        d.setSvrId(s.getSvrId());
        d.setSubmissionId(s.getSubmissionId());
        d.setContestId(s.getContestId());
        d.setRanking(s.getRanking());
        d.setDateCreated(s.getDateCreated());
        return d;
    }

}
