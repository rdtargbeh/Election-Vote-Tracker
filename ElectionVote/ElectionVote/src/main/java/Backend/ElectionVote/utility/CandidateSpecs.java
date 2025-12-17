package Backend.ElectionVote.utility;

import Backend.ElectionVote.entity.Candidate;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class CandidateSpecs {
    private CandidateSpecs() {}

    public static Specification<Candidate> nameContains(String q) {
        return (root, cq, cb) ->
                (q == null || q.isBlank())
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("fullName")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<Candidate> positionContains(String pos) {
        return (root, cq, cb) ->
                (pos == null || pos.isBlank())
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("position")), "%" + pos.toLowerCase() + "%");
    }

    public static Specification<Candidate> partyEquals(UUID partyId) {
        return (root, cq, cb) ->
                (partyId == null)
                        ? cb.conjunction()
                        : cb.equal(root.get("party").get("partyId"), partyId);
    }

    public static Specification<Candidate> activeEquals(Boolean active) {
        return (root, cq, cb) ->
                (active == null)
                        ? cb.conjunction()
                        : cb.equal(root.get("isActive"), active);
    }
}