package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.ElectionCandidateCreateRequest;
import Backend.ElectionVote.dto.ElectionCandidateDto;
import Backend.ElectionVote.dto.ElectionCandidateUpdateRequest;

import Backend.ElectionVote.entity.Candidate;
import Backend.ElectionVote.entity.Election;
import Backend.ElectionVote.entity.ElectionCandidate;
import Backend.ElectionVote.entity.PollingCenter;
import org.springframework.stereotype.Component;

@Component
public class ElectionCandidateMapper {

    public ElectionCandidateDto toDTO(ElectionCandidate ec) {
        if (ec == null) return null;
        return ElectionCandidateDto.builder()
                .electId(ec.getElectId())
                .electionId(ec.getElection() != null ? ec.getElection().getElectionId() : null)
                .electionName(ec.getElection() != null ? ec.getElection().getElectionName() : null)
                .candidateId(ec.getCandidate() != null ? ec.getCandidate().getCandidateId() : null)
                .fullName(ec.getCandidate() != null ? ec.getCandidate().getFullName() : null)
                .centerId(ec.getPollingCenter() != null ? ec.getPollingCenter().getCenterId() : null)
                .centerName(ec.getPollingCenter() != null ? ec.getPollingCenter().getCenterName() : null)
                .build();
    }

    public ElectionCandidate toEntity(
            ElectionCandidateCreateRequest req,
            Election election,
            Candidate candidate,
            PollingCenter pollingCenter
    ) {
        if (req == null) return null;
        return ElectionCandidate.builder()
                .election(election)
                .candidate(candidate)
                .pollingCenter(pollingCenter)
                .build();
    }

    public void apply(ElectionCandidateUpdateRequest req, ElectionCandidate ec, PollingCenter newCenter) {
        if (req == null || ec == null) return;
        ec.setPollingCenter(newCenter);
    }
}