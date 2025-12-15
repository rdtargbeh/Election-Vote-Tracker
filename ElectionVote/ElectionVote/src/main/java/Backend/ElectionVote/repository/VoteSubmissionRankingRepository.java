package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.VoteSubmissionRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoteSubmissionRankingRepository extends JpaRepository<VoteSubmissionRanking, UUID> {

    List<VoteSubmissionRanking> findBySubmissionId(UUID submissionId);
    List<VoteSubmissionRanking> findByContestId(UUID contestId);
    void deleteBySubmissionId(UUID submissionId);
    Optional<VoteSubmissionRanking> findBySubmissionIdAndContestId(UUID submissionId, UUID contestId);

}
