package Backend.ElectionVote.utility;


import Backend.ElectionVote.entity.County;
import Backend.ElectionVote.entity.District;
import Backend.ElectionVote.entity.PollingCenter;
import Backend.ElectionVote.entity.VoteSubmission;
import Backend.ElectionVote.enums.ContestCategory;
import Backend.ElectionVote.enums.ContestScopeType;
import Backend.ElectionVote.enums.VoteStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public final class VoteSubmissionSpecs {

    private VoteSubmissionSpecs(){}

    public static Specification<VoteSubmission> orgEquals(UUID orgId){
        return (r, q, cb) -> orgId == null
                ? cb.conjunction()
                : cb.equal(r.get("organization").get("orgId"), orgId);
    }

    public static Specification<VoteSubmission> electionEquals(UUID electionId){
        return (r, q, cb) -> electionId == null
                ? cb.conjunction()
                : cb.equal(r.get("election").get("electionId"), electionId);
    }

    public static Specification<VoteSubmission> centerEquals(UUID centerId){
        return (r, q, cb) -> centerId == null
                ? cb.conjunction()
                // ✅ FIX: PollingCenter entity typically has "centerId"
                : cb.equal(r.get("pollingCenter").get("centerId"), centerId);
        // If your PollingCenter uses pollingCenterId, change to:
        // : cb.equal(r.get("pollingCenter").get("pollingCenterId"), centerId);
    }

    public static Specification<VoteSubmission> agentEquals(UUID agentId){
        return (r, q, cb) -> agentId == null
                ? cb.conjunction()
                : cb.equal(r.get("agent").get("userId"), agentId);
    }

    public static Specification<VoteSubmission> statusEquals(VoteStatus status){
        return (r, q, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(r.get("status"), status);
    }

    public static Specification<VoteSubmission> between(LocalDateTime from, LocalDateTime to){
        return (r, q, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from != null && to != null) return cb.between(r.get("submissionTime"), from, to);
            return from != null
                    ? cb.greaterThanOrEqualTo(r.get("submissionTime"), from)
                    : cb.lessThanOrEqualTo(r.get("submissionTime"), to);
        };
    }

    public static Specification<VoteSubmission> textSearch(String q){
        return (r, qy, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String like = "%" + q.toLowerCase() + "%";
            return cb.like(cb.lower(r.get("comments")), like);
        };
    }

    // ------------------------------------------------------------------
    // ✅ Contest linkage filters (requires vote_submission.contestId + relation "contest")
    // ------------------------------------------------------------------

//    /** Filter direct contest_id on VoteSubmission (fast, no join needed). */
//    public static Specification<VoteSubmission> contestEquals(UUID contestId) {
//        return (root, query, cb) ->
//                contestId == null ? cb.conjunction() : cb.equal(root.get("contestId"), contestId);
//    }

    /** Filter by Contest.category via join. */
    public static Specification<VoteSubmission> contestCategoryEquals(ContestCategory category) {
        return (root, query, cb) -> {
            if (category == null) return cb.conjunction();
            return cb.equal(root.join("contest", JoinType.INNER).get("category"), category);
        };
    }

    /** Filter by Contest.scopeType via join. */
    public static Specification<VoteSubmission> contestScopeEquals(ContestScopeType scopeType) {
        return (root, query, cb) -> {
            if (scopeType == null) return cb.conjunction();
            return cb.equal(root.join("contest", JoinType.INNER).get("scopeType"), scopeType);
        };
    }

    /** Optional: Filter by Contest.countyId (useful for SENATE by county). */
    public static Specification<VoteSubmission> contestCountyEquals(UUID countyId) {
        return (root, query, cb) -> {
            if (countyId == null) return cb.conjunction();
            return cb.equal(root.join("contest", JoinType.INNER).get("countyId"), countyId);
        };
    }

    /** Optional: Filter by Contest.districtId (useful for REPRESENTATIVE by district). */
    public static Specification<VoteSubmission> contestDistrictEquals(UUID districtId) {
        return (root, query, cb) -> {
            if (districtId == null) return cb.conjunction();
            return cb.equal(root.join("contest", JoinType.INNER).get("districtId"), districtId);
        };
    }



    /**
     * ✅ Filter submission by LOCATION county:
     * VoteSubmission -> pollingCenter -> district -> county
     */
    public static Specification<VoteSubmission> countyEquals(UUID countyId) {
        return (root, query, cb) -> {
            if (countyId == null) return cb.conjunction();

            // Avoid duplicates when joining
            query.distinct(true);

            return cb.equal(
                    root.join("pollingCenter", JoinType.LEFT)
                            .join("district", JoinType.LEFT)
                            .join("county", JoinType.LEFT)
                            .get("countyId"),
                    countyId
            );
        };
    }

    /**
     * ✅ Filter submission by LOCATION district:
     * VoteSubmission -> pollingCenter -> district
     */
    public static Specification<VoteSubmission> districtEquals(UUID districtId) {
        return (root, query, cb) -> {
            if (districtId == null) return cb.conjunction();

            query.distinct(true);

            return cb.equal(
                    root.join("pollingCenter", JoinType.LEFT)
                            .join("district", JoinType.LEFT)
                            .get("districtId"),
                    districtId
            );
        };
    }

    /**
     * ✅ Contest dropdown filter:
     * Use ONE of the two options depending on your entity mapping.
     */
    public static Specification<VoteSubmission> contestEquals(UUID contestId) {
        return (root, query, cb) -> {
            if (contestId == null) return cb.conjunction();

            // OPTION A: if VoteSubmission has a UUID contestId column field:
            // return cb.equal(root.get("contestId"), contestId);

            // OPTION B: if VoteSubmission has @ManyToOne Contest contest;
            return cb.equal(root.join("contest", JoinType.INNER).get("contestId"), contestId);
        };
    }


}
