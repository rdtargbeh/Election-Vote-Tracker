package Backend.ElectionVote.service.implement;


import Backend.ElectionVote.repository.DiscrepancyReconcileRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DiscrepancyReconcileRepositoryImpl implements DiscrepancyReconcileRepository {

    private final EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<Row> findMismatches(UUID orgId, UUID electionId) {
        String sql = """
            SELECT p.election_id,
                   p.center_id,
                   p.valid_votes     AS party_valid,
                   p.invalid_total   AS party_invalid,
                   o.valid_votes     AS official_valid,
                   o.invalid_total   AS official_invalid
            FROM v_center_stats_party    p
            JOIN v_center_stats_official o
              ON o.election_id = p.election_id AND o.center_id = p.center_id
            WHERE p.org_id = :orgId
              AND p.election_id = :electionId
              AND (p.valid_votes   IS DISTINCT FROM o.valid_votes
                   OR p.invalid_total IS DISTINCT FROM o.invalid_total)
            """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("orgId", orgId)
                .setParameter("electionId", electionId)
                .getResultList();

        List<Row> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            out.add(new Row(
                    (UUID) r[0],
                    (UUID) r[1],
                    ((Number) r[2]).intValue(),
                    ((Number) r[3]).intValue(),
                    ((Number) r[4]).intValue(),
                    ((Number) r[5]).intValue()
            ));
        }
        return out;
    }
}
