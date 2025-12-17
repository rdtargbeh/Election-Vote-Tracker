package Backend.ElectionVote.utility;

import Backend.ElectionVote.views.NecResultGeo;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public final class NecResultGeoSpecs {
    private NecResultGeoSpecs() {}

    public static Specification<NecResultGeo> electionEquals(UUID electionId) {
        return (root, cq, cb) -> electionId == null ? cb.conjunction() :
                cb.equal(root.get("electionId"), electionId);
    }
    public static Specification<NecResultGeo> countyEquals(UUID countyId) {
        return (root, cq, cb) -> countyId == null ? cb.conjunction() :
                cb.equal(root.get("countyId"), countyId);
    }
    public static Specification<NecResultGeo> districtEquals(UUID districtId) {
        return (root, cq, cb) -> districtId == null ? cb.conjunction() :
                cb.equal(root.get("districtId"), districtId);
    }
    public static Specification<NecResultGeo> centerEquals(UUID centerId) {
        return (root, cq, cb) -> centerId == null ? cb.conjunction() :
                cb.equal(root.get("centerId"), centerId);
    }
    public static Specification<NecResultGeo> uploadedAfter(LocalDateTime t) {
        return (root, cq, cb) -> t == null ? cb.conjunction() :
                cb.greaterThanOrEqualTo(root.get("uploadTime"), t);
    }
    public static Specification<NecResultGeo> uploadedBefore(LocalDateTime t) {
        return (root, cq, cb) -> t == null ? cb.conjunction() :
                cb.lessThanOrEqualTo(root.get("uploadTime"), t);
    }
    public static Specification<NecResultGeo> textSearch(String q) {
        return (root, cq, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String like = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("centerName")), like),
                    cb.like(cb.lower(root.get("centerCode")), like),
                    cb.like(cb.lower(root.get("districtName")), like),
                    cb.like(cb.lower(root.get("countyName")), like)
            );
        };
    }
}
