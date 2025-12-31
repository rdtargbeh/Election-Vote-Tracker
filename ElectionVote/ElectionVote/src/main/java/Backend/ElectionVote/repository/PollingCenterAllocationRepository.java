package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.Election;
import Backend.ElectionVote.entity.PollingCenter;
import Backend.ElectionVote.entity.PollingCenterAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollingCenterAllocationRepository  extends JpaRepository<PollingCenterAllocation, UUID>,
        JpaSpecificationExecutor<PollingCenterAllocation> {

    Optional<PollingCenterAllocation> findByElection_ElectionIdAndPollingCenter_CenterId(
            UUID electionId, UUID centerId);

    boolean existsByElection_ElectionIdAndPollingCenter_CenterId(
            UUID electionId, UUID centerId);

    Optional<PollingCenterAllocation> findByElectionAndPollingCenter(
            Election election,
            PollingCenter pollingCenter
    );



}