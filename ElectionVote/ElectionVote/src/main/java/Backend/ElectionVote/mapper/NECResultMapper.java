package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.NECResultCreateRequest;
import Backend.ElectionVote.dto.NECResultDto;
import Backend.ElectionVote.dto.NECResultUpdateRequest;
import Backend.ElectionVote.entity.Election;
import Backend.ElectionVote.entity.NECResult;
import Backend.ElectionVote.entity.PollingCenter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Component
public class NECResultMapper {

    private static final ObjectMapper M = new ObjectMapper();
    private static final TypeReference<Map<UUID, Integer>> T = new TypeReference<>() {};

    public NECResultDto toDTO(NECResult r) {
        if (r == null) return null;
        return NECResultDto.builder()
                .resultId(r.getResultId())
                .electionId(r.getElection() != null ? r.getElection().getElectionId() : null)
                .electionName(r.getElection() != null ? r.getElection().getElectionName() : null)
                .centerId(r.getPollingCenter() != null ? r.getPollingCenter().getCenterId() : null)
                .pollingCenterName(r.getPollingCenter() != null ? r.getPollingCenter().getCenterName() : null)
                .candidateVotes(parse(r.getCandidateVotes()))
                .totalRegisteredVoters(r.getTotalRegisteredVoters())
                .ballotsCast(r.getBallotsCast())
                .invalidBallots(r.getInvalidBallots())
                .blankBallots(r.getBlankBallots())
                .rejectedBallots(r.getRejectedBallots())
                .spoiledBallots(r.getSpoiledBallots())
                .source(r.getSource())
                .uploadTime(r.getUploadTime())
                .build();
    }

    public NECResult toEntity(NECResultCreateRequest req, Election e, PollingCenter c) {
        NECResult r = new NECResult();
        r.setElection(e);
        r.setPollingCenter(c);
        r.setCandidateVotes(write(req.getCandidateVotes()));
        r.setTotalRegisteredVoters(nz(req.getTotalRegisteredVoters()));
        r.setBallotsCast(req.getBallotsCast());
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

    private static String write(Map<UUID, Integer> m) {
        try { return M.writeValueAsString(m == null ? Collections.emptyMap() : m); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid candidateVotes", e); }
    }
    private static Map<UUID,Integer> parse(String json) {
        try { return (json == null || json.isBlank()) ? Collections.emptyMap() : M.readValue(json, T); }
        catch (Exception e) { throw new IllegalStateException("Bad candidateVotes json", e); }
    }
    private static int nz(Integer x) { return x == null ? 0 : x; }


}