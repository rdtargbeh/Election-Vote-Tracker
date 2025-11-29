package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.VoteDetailCreateRequest;
import Backend.ElectionVote.dto.VoteDetailDto;
import Backend.ElectionVote.dto.VoteDetailUpdateRequest;
import Backend.ElectionVote.entity.*;
import org.springframework.stereotype.Component;

@Component
public class VoteDetailMapper {

    public VoteDetailDto toDTO(VoteDetail v) {
        Party p = v.getParty();
        Candidate c = v.getCandidate();
        return VoteDetailDto.builder()
                .detailId(v.getDetailId())
                .orgId(v.getOrganization().getOrgId())
                .submissionId(v.getSubmission().getSubmissionId())
                .partyId(p != null ? p.getPartyId() : null)
                .partyName(p != null ? p.getPartyName() : null)
                .partyAbbreviation(p != null ? p.getAbbreviation() : null)
                .candidateId(c != null ? c.getCandidateId() : null)
                .candidateName(c != null ? c.getFullName() : null)
                .voteCount(v.getVoteCount())
                .build();
    }

    public VoteDetail toEntity(VoteDetailCreateRequest req,
                               Organization org, VoteSubmission sub,
                               Party party, Candidate candidate) {
        VoteDetail v = new VoteDetail();
        v.setOrganization(org);
        v.setSubmission(sub);
        v.setParty(party);
        v.setCandidate(candidate);
        v.setVoteCount(req.getVoteCount());
        return v;
    }

    public void apply(VoteDetailUpdateRequest req, VoteDetail v,
                      Party party, Candidate candidate) {
        if (req.getVoteCount() != null) v.setVoteCount(req.getVoteCount());
        if (party != null || candidate != null) {
            v.setParty(party);
            v.setCandidate(candidate);
        }
    }
}
