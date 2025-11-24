package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.PollingPlaceAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PollingPlaceAllocationRepository
        extends JpaRepository<PollingPlaceAllocation, UUID>,
        JpaSpecificationExecutor<PollingPlaceAllocation> {

    Optional<PollingPlaceAllocation> findByElection_ElectionIdAndPollingPlace_PlaceId(UUID electionId, UUID placeId);

    boolean existsByElection_ElectionIdAndPollingPlace_PlaceId(UUID electionId, UUID placeId);

}



