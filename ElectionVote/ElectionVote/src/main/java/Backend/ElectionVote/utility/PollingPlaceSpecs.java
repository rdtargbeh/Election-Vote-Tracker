package Backend.ElectionVote.utility;

import Backend.ElectionVote.entity.PollingPlace;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.UUID;

public final class PollingPlaceSpecs {

    private PollingPlaceSpecs() {}


    public static Specification<PollingPlace> filter(
            String q, UUID countyId, UUID districtId, UUID centerId, Boolean active
    ) {
        return (root, query, cb) -> {

            var center = root.join("pollingCenter");
            var district = center.join("district");
            var county = district.join("county");

            var preds = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (active != null) preds.add(cb.equal(root.get("active"), active));
            if (centerId != null) preds.add(cb.equal(center.get("centerId"), centerId));
            if (districtId != null) preds.add(cb.equal(district.get("districtId"), districtId));
            if (countyId != null) preds.add(cb.equal(county.get("countyId"), countyId));

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("label")), like),
                        cb.like(cb.lower(center.get("centerName")), like),
                        cb.like(cb.lower(center.get("code")), like)
                ));
            }

            // ✅ REMOVE THIS (Postgres DISTINCT + ORDER BY join-field error)
            // query.distinct(true);

            return cb.and(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }


}
