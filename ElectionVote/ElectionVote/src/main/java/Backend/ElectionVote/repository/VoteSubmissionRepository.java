package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.VoteSubmission;
import Backend.ElectionVote.enums.VoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoteSubmissionRepository
        extends JpaRepository<VoteSubmission, UUID>, JpaSpecificationExecutor<VoteSubmission> {

    boolean existsBySubmissionHash(String submissionHash);

    Optional<VoteSubmission> findByIdempotencyKey(String idempotencyKey);

    List<VoteSubmission> findByOrganization_OrgIdAndElection_ElectionIdAndStatus(
            UUID orgId,
            UUID electionId,
            VoteStatus status
    );


    // helpful lookup patterns
    Optional<VoteSubmission> findFirstByOrganization_OrgIdAndElection_ElectionIdAndPollingCenter_CenterIdAndAgent_UserIdOrderBySubmissionTimeDesc(
            UUID orgId, UUID electionId, UUID centerId, UUID agentId);

    Optional<VoteSubmission> findBySubmissionHash(String submissionHash);



}
