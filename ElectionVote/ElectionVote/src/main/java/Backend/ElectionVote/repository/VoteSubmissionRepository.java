package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.VoteSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoteSubmissionRepository
        extends JpaRepository<VoteSubmission, UUID>, JpaSpecificationExecutor<VoteSubmission> {

    boolean existsBySubmissionHash(String submissionHash);

    // helpful lookup patterns
    Optional<VoteSubmission> findFirstByOrganization_OrgIdAndElection_ElectionIdAndPollingCenter_CenterIdAndAgent_UserIdOrderBySubmissionTimeDesc(
            UUID orgId, UUID electionId, UUID centerId, UUID agentId);
}
