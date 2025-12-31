package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.*;
import Backend.ElectionVote.entity.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Unified mapper for NEC result-related entities and DTOs.
 *
 * Notes:
 * - Candidate votes JSON is stored in the DB as jsonb. We normalize in Java to Map<String,Integer>
 *   where candidate id keys are stringified (UUID.toString()) to avoid mapping issues with JSON keys.
 * - Methods accept generic Map<?,Integer> when converting from external requests to be forgiving.
 */
@Component
public class NECResultMapper {

    private static final ObjectMapper M = new ObjectMapper();
    private static final TypeReference<Map<String, Integer>> TR = new TypeReference<>() {};

    // -------------------------
    // NECResult (authoritative) <-> DTO
    // -------------------------
    public NECResultDto toDTO(NECResult r) {

        if (r == null) return null;
        Map<String, Integer> votes = parseToMapString(r.getCandidateVotes());
        return NECResultDto.builder()
                .resultId(r.getResultId())
                .electionId(r.getElection() != null ? r.getElection().getElectionId() : null)
                .electionName(r.getElection() != null ? r.getElection().getElectionName() : null)
                .centerId(r.getPollingCenter() != null ? r.getPollingCenter().getCenterId() : null)
                .pollingCenterName(r.getPollingCenter() != null ? r.getPollingCenter().getCenterName() : null)
                .candidateVotes(votes)
                .totalRegisteredVoters(nz(r.getTotalRegisteredVoters()))
                .ballotsCast(nz(r.getBallotsCast()))
                .invalidBallots(nz(r.getInvalidBallots()))
                .blankBallots(nz(r.getBlankBallots()))
                .rejectedBallots(nz(r.getRejectedBallots()))
                .spoiledBallots(nz(r.getSpoiledBallots()))
                .source(r.getSource())
                .uploadTime(r.getUploadTime())
                .build();
    }


    /**
     * Build entity from create request. Accepts candidateVotes as Map with keys that can be UUID or String.
     */
    public NECResult toEntity(NECResultCreateRequest req, Election e, PollingCenter c) {
        NECResult r = new NECResult();
        r.setElection(e);
        r.setPollingCenter(c);
        r.setCandidateVotes(write(req.getCandidateVotes()));
        r.setTotalRegisteredVoters(nz(req.getTotalRegisteredVoters()));
        r.setBallotsCast(nz(req.getBallotsCast()));
        r.setInvalidBallots(nz(req.getInvalidBallots()));
        r.setBlankBallots(nz(req.getBlankBallots()));
        r.setRejectedBallots(nz(req.getRejectedBallots()));
        r.setSpoiledBallots(nz(req.getSpoiledBallots()));
        r.setSource(req.getSource());
        return r;
    }

    public void apply(NECResultUpdateRequest req, NECResult r) {
        if (req == null || r == null) return;
        if (req.getCandidateVotes() != null) r.setCandidateVotes(write(req.getCandidateVotes()));
        if (req.getTotalRegisteredVoters() != null) r.setTotalRegisteredVoters(req.getTotalRegisteredVoters());
        if (req.getBallotsCast() != null)    r.setBallotsCast(req.getBallotsCast());
        if (req.getInvalidBallots() != null) r.setInvalidBallots(req.getInvalidBallots());
        if (req.getBlankBallots() != null)   r.setBlankBallots(req.getBlankBallots());
        if (req.getRejectedBallots() != null)r.setRejectedBallots(req.getRejectedBallots());
        if (req.getSpoiledBallots() != null) r.setSpoiledBallots(req.getSpoiledBallots());
        if (req.getSource() != null)         r.setSource(req.getSource());
    }

    // -------------------------
    // NecResultStaging <-> DTO
    // -------------------------
    public NecResultStagingDto toDto(NecResultStaging s) {
        if (s == null) return null;
        NecResultStagingDto d = new NecResultStagingDto();
        d.setStagingId(s.getStagingId());
        d.setBatchId(s.getBatchId());
        d.setElectionId(s.getElectionId());
        d.setCenterCode(s.getCenterCode());
        d.setAssignedCenterId(s.getAssignedCenterId());
        d.setCandidateVotes(s.getCandidateVotes());
        d.setTotalRegisteredVoters(s.getTotalRegisteredVoters());
        d.setBallotsCast(s.getBallotsCast());
        d.setInvalidBallots(s.getInvalidBallots());
        d.setBlankBallots(s.getBlankBallots());
        d.setRejectedBallots(s.getRejectedBallots());
        d.setSpoiledBallots(s.getSpoiledBallots());
        d.setSource(s.getSource());
        d.setUploadedBy(s.getUploadedBy() != null ? s.getUploadedBy().getUserId() : null);
        d.setUploadTime(s.getUploadTime());
        d.setValidated(s.getValidated());
        d.setValidationErrors(s.getValidationErrors());
        d.setValidatedBy(s.getValidatedBy() != null ? s.getValidatedBy().getUserId() : null);
        d.setValidatedAt(s.getValidatedAt());
        d.setIsPublished(s.getIsPublished());
        d.setPublishedBy(s.getPublishedBy() != null ? s.getPublishedBy().getUserId() : null);
        d.setPublishedAt(s.getPublishedAt());
        d.setProcessed(s.getProcessed());
        d.setProcessedAt(s.getProcessedAt());
        d.setProcessedResultId(s.getProcessedResultId());
        return d;
    }

    // -------------------------
    // NecResultPublic <-> DTO
//    // -------------------------
//    public NecResultPublicDto toPublicDto(NecResultPublic p) {
//        if (p == null) return null;
//        NecResultPublicDto d = new NecResultPublicDto();
//        d.setElectionId(p.getElectionId());
//        d.setAssignedCenterId(p.getAssignedCenterId());
//        d.setCenterCode(p.getCenterCode());
//        d.setCandidateVotes(p.getCandidateVotes());
//        d.setTotalRegisteredVoters(p.getTotalRegisteredVoters());
//        d.setBallotsCast(p.getBallotsCast());
//        d.setInvalidBallots(p.getInvalidBallots());
//        d.setBlankBallots(p.getBlankBallots());
//        d.setRejectedBallots(p.getRejectedBallots());
//        d.setSpoiledBallots(p.getSpoiledBallots());
//        d.setSource(p.getSource());
//        d.setPublishedAt(p.getPublishedAt());
//        return d;
//    }

    // -------------------------
    // JSON helpers - use Map<String,Integer> consistently
    // -------------------------
    private static String write(Map<?, Integer> map) {
        try {
            if (map == null) return M.writeValueAsString(Collections.emptyMap());
            // Convert keys to String (UUID or plain strings) to ensure JSON keys are strings
            Map<String, Integer> asStrings = new HashMap<>();
            for (Map.Entry<?, Integer> e : map.entrySet()) {
                asStrings.put(e.getKey() == null ? "" : e.getKey().toString(), e.getValue() == null ? 0 : e.getValue());
            }
            return M.writeValueAsString(asStrings);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid candidateVotes", e);
        }
    }

    public static Map<String, Integer> parseToMapString(String json) {
        try {
            if (json == null || json.isBlank()) return Collections.emptyMap();
            return M.readValue(json, TR);
        } catch (Exception e) {
            throw new IllegalStateException("Bad candidateVotes json", e);
        }
    }


    private static int nz(Integer x) { return x == null ? 0 : x; }
}


