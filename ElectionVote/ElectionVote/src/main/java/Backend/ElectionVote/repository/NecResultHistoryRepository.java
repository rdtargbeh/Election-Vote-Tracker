package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.NecResultHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NecResultHistoryRepository extends JpaRepository<NecResultHistory, UUID> {
    List<NecResultHistory> findByResultIdOrderByChangedAtDesc(UUID resultId);
    List<NecResultHistory> findByElectionIdOrderByChangedAtDesc(UUID electionId);

}
