package Backend.ElectionVote.utility;


import Backend.ElectionVote.entity.VoteSubmission;
import Backend.ElectionVote.enums.VoteStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public final class VoteSubmissionSpecs {
    private VoteSubmissionSpecs(){}

    public static Specification<VoteSubmission> orgEquals(UUID orgId){
        return (r, q, cb) -> orgId==null? cb.conjunction(): cb.equal(r.get("organization").get("orgId"), orgId);
    }
    public static Specification<VoteSubmission> electionEquals(UUID electionId){
        return (r, q, cb) -> electionId==null? cb.conjunction(): cb.equal(r.get("election").get("electionId"), electionId);
    }
    public static Specification<VoteSubmission> centerEquals(UUID centerId){
        return (r, q, cb) -> centerId==null? cb.conjunction(): cb.equal(r.get("pollingCenter").get("pollingCenterId"), centerId);
    }
    public static Specification<VoteSubmission> agentEquals(UUID agentId){
        return (r, q, cb) -> agentId==null? cb.conjunction(): cb.equal(r.get("agent").get("userId"), agentId);
    }
    public static Specification<VoteSubmission> statusEquals(VoteStatus status){
        return (r, q, cb) -> status==null? cb.conjunction(): cb.equal(r.get("status"), status);
    }
    public static Specification<VoteSubmission> between(LocalDateTime from, LocalDateTime to){
        return (r, q, cb) -> {
            if (from==null && to==null) return cb.conjunction();
            if (from!=null && to!=null) return cb.between(r.get("submissionTime"), from, to);
            return from!=null? cb.greaterThanOrEqualTo(r.get("submissionTime"), from)
                    : cb.lessThanOrEqualTo(r.get("submissionTime"), to);
        };
    }
    public static Specification<VoteSubmission> textSearch(String q){
        return (r, qy, cb) -> {
            if (q==null || q.isBlank()) return cb.conjunction();
            String like = "%" + q.toLowerCase() + "%";
            return cb.like(cb.lower(r.get("comments")), like);
        };
    }
}
