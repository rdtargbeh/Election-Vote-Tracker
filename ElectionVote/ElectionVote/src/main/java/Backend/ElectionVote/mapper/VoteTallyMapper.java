package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.VoteTallyCreateRequest;
import Backend.ElectionVote.dto.VoteTallyDto;
import Backend.ElectionVote.dto.VoteTallyUpdateRequest;
import Backend.ElectionVote.entity.*;
import org.springframework.stereotype.Component;

@Component
public class VoteTallyMapper {

    public VoteTallyDto toDTO(VoteTally v) {
        Party p = v.getParty();
        Candidate c = v.getCandidate();
        Organization o = v.getOrganization();
        Election e = v.getElection();

        return VoteTallyDto.builder()
                .tallyId(v.getTallyId())
                .orgId(v.getOrganization().getOrgId())
                .orgName(o != null ? o.getOrgName() : null)
                .partyId(p != null ? p.getPartyId() : null)
                .partyName(p != null ? p.getPartyName() : null)
                .abbreviation(p != null ? p.getAbbreviation() : null)
                .candidateId(c != null ? c.getCandidateId() : null)
                .fullName(c != null ? c.getFullName() : null)
                .electionId(e != null ? e.getElectionId() : null)
                .electionName(e != null ? e.getElectionName() : null)
                .voteCount(v.getVoteCount())
                .lastRecomputedAt(v.getLastRecomputedAt())
                .recomputedByUserId(v.getRecomputedBy().getUserId())

//                .submissionId(v.getSubmission().getSubmissionId())

                .build();
    }


    public VoteTally toEntity(VoteTallyCreateRequest req,
                              Organization org, // VoteSubmission sub,
                              Party party, Candidate candidate, Election elect) {
        VoteTally v = new VoteTally();
        v.setOrganization(org);
//        v.setSubmission(sub);
        v.setParty(party);
        v.setCandidate(candidate);
        v.setElection(elect);
        v.setVoteCount(req.getVoteCount());
        return v;
    }

    public void apply(VoteTallyUpdateRequest req, VoteTally v,
                      Party party, Candidate candidate) {
        if (req.getVoteCount() != null) v.setVoteCount(req.getVoteCount());
        if (party != null || candidate != null) {
            v.setParty(party);
            v.setCandidate(candidate);
        }
    }
}
