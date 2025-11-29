package Backend.ElectionVote.utility;

import Backend.ElectionVote.entity.VoteDetail;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class VoteDetailSpecs {
    private VoteDetailSpecs(){}

    public static Specification<VoteDetail> orgEquals(UUID orgId){
        return (r,q,cb) -> orgId==null? cb.conjunction()
                : cb.equal(r.get("organization").get("orgId"), orgId);
    }
    public static Specification<VoteDetail> submissionEquals(UUID submissionId){
        return (r,q,cb) -> submissionId==null? cb.conjunction()
                : cb.equal(r.get("submission").get("submissionId"), submissionId);
    }
    public static Specification<VoteDetail> candidateEquals(UUID candidateId){
        return (r,q,cb) -> candidateId==null? cb.conjunction()
                : cb.equal(r.get("candidate").get("candidateId"), candidateId);
    }
    public static Specification<VoteDetail> partyEquals(UUID partyId){
        return (r,q,cb) -> partyId==null? cb.conjunction()
                : cb.equal(r.get("party").get("partyId"), partyId);
    }
}

