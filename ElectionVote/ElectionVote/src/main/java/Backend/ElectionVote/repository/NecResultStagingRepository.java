package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.NecResultStaging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NecResultStagingRepository extends JpaRepository<NecResultStaging, UUID> {
    List<NecResultStaging> findByElectionId(UUID electionId);
    List<NecResultStaging> findByElectionIdAndValidatedFalse(UUID electionId);
    List<NecResultStaging> findByElectionIdAndValidatedTrueAndIsPublishedFalse(UUID electionId);

    // NEW methods used by ImportBatchServiceImpl
    List<NecResultStaging> findByBatchId(UUID batchId);
    List<NecResultStaging> findByBatchIdAndValidatedTrue(UUID batchId);
    List<NecResultStaging> findByBatchIdAndValidatedTrueAndProcessedFalse(UUID batchId);
    List<NecResultStaging> findByBatchIdAndProcessedTrue(UUID batchId);

    // Pageable chunked fetch used by async worker
    Page<NecResultStaging> findByBatchIdAndValidatedTrueAndProcessedFalse(UUID batchId, Pageable pageable);


}