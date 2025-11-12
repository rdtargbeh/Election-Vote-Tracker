package Backend.ElectionVote.repository;

import lombok.Value;

import java.util.List;
import java.util.UUID;


public interface DiscrepancyReconcileRepository {

    @Value
    class Row {
        UUID electionId;
        UUID centerId;
        Integer partyValid;
        Integer partyInvalid;
        Integer officialValid;
        Integer officialInvalid;
    }

    List<Row> findMismatches(UUID orgId, UUID electionId);
}

