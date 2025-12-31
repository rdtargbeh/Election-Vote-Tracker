package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.VoterRegistrationStaging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VoterRegistrationStagingRepository extends JpaRepository<VoterRegistrationStaging, UUID> {
    List<VoterRegistrationStaging> findByBatchId(UUID batchId);
    List<VoterRegistrationStaging> findByValidatedFalseAndProcessedFalse();
    List<VoterRegistrationStaging> findByProcessedFalseAndValidatedTrue();
}