package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.VoteSubmissionContest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VoteSubmissionContestRepository extends JpaRepository<VoteSubmissionContest, UUID> {
    List<VoteSubmissionContest> findBySubmissionId(UUID submissionId);
    List<VoteSubmissionContest> findByElectionIdAndContestId(UUID electionId, UUID contestId);
    void deleteBySubmissionId(UUID submissionId);
}