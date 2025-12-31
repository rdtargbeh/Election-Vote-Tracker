package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.ElectionCandidateCreateRequest;
import Backend.ElectionVote.dto.ElectionCandidateDto;
import Backend.ElectionVote.dto.ElectionCandidateUpdateRequest;

import Backend.ElectionVote.entity.*;
import org.springframework.stereotype.Component;

@Component
public class ElectionCandidateMapper {


    public ElectionCandidateDto toDTO(ElectionCandidate ec) {
        if (ec == null) return null;

        Candidate c = ec.getCandidate();
        Party p = (c != null) ? c.getParty() : null;
        Election e = ec.getElection();
        PollingCenter pc = ec.getPollingCenter();

        return ElectionCandidateDto.builder()
                .electId(ec.getElectId())
                .electionId(e != null ? e.getElectionId() : null)
                .electionName(e != null ? e.getElectionName() : null)
                .centerId(pc != null ? pc.getCenterId() : null)
                .centerName(pc != null ? pc.getCenterName() : null)
                .candidateId(c != null ? c.getCandidateId() : null)
                .fullName(c != null ? c.getFullName() : null)
                .partyId(p != null ? p.getPartyId() : null)
                .partyAbbrev(p != null ? p.getAbbreviation() : null)
                .dateCreated(ec.getDateCreated())
                .dateUpdated(ec.getDateUpdated())

                .build();
    }
//
//    public ElectionCandidateDto toDTO(ElectionCandidate ec) {
//        if (ec == null) return null;
//
//        Candidate c = ec.getCandidate();
//        Party p = (c != null) ? c.getParty() : null;
//
//        return ElectionCandidateDto.builder()
//                .electId(ec.getElectId())
//                .electionId(
//                        ec.getElection() != null ? ec.getElection().getElectionId() : null
//                )
//                .centerId(
//                        ec.getPollingCenter() != null ? ec.getPollingCenter().getCenterId() : null
//                )
//                .candidateId(c != null ? c.getCandidateId() : null)
//                .fullName(c != null ? c.getFullName() : null)
//                .partyId(p != null ? p.getPartyId() : null)
//                .partyAbbrev(p != null ? p.getAbbreviation() : null)  // <<< here
//                .build();
//    }



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