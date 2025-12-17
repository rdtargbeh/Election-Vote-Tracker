package Backend.ElectionVote.utility;

import Backend.ElectionVote.entity.VoteTally;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class VoteTallySpecs {
    private VoteTallySpecs(){}

    public static Specification<VoteTally> orgEquals(UUID orgId){
        return (r,q,cb) -> orgId==null? cb.conjunction()
                : cb.equal(r.get("organization").get("orgId"), orgId);
    }
    public static Specification<VoteTally> submissionEquals(UUID submissionId){
        return (r,q,cb) -> submissionId==null? cb.conjunction()
                : cb.equal(r.get("submission").get("submissionId"), submissionId);
    }
    public static Specification<VoteTally> candidateEquals(UUID candidateId){
        return (r,q,cb) -> candidateId==null? cb.conjunction()
                : cb.equal(r.get("candidate").get("candidateId"), candidateId);
    }
    public static Specification<VoteTally> partyEquals(UUID partyId){
        return (r,q,cb) -> partyId==null? cb.conjunction()
                : cb.equal(r.get("party").get("partyId"), partyId);
    }

    public static Specification<VoteTally> electionEquals(UUID electionId){
        return (r,q,cb) -> electionId==null? cb.conjunction()
                : cb.equal(r.get("election").get("electionId"), electionId);
    }
}

