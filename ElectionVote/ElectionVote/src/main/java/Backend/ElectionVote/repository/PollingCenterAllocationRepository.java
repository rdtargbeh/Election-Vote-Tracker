package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.PollingCenterAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollingCenterAllocationRepository extends JpaRepository<PollingCenterAllocation, UUID> {

    Optional<PollingCenterAllocation> findByElection_ElectionIdAndPollingCenter_CenterId(
            UUID electionId, UUID centerId);

    boolean existsByElection_ElectionIdAndPollingCenter_CenterId(UUID electionId, UUID centerId);
}