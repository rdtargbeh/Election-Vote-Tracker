package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.VoteDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VoteDetailRepository
        extends JpaRepository<VoteDetail, UUID>, JpaSpecificationExecutor<VoteDetail> {

    List<VoteDetail> findBySubmission_SubmissionId(UUID submissionId);

    @Modifying
    @Query("delete from VoteDetail v where v.submission.submissionId = :submissionId")
    void deleteBySubmissionId(@Param("submissionId") UUID submissionId);

    @Query("select coalesce(sum(v.voteCount),0) from VoteDetail v where v.submission.submissionId = :submissionId")
    long sumVotesBySubmission(@Param("submissionId") UUID submissionId);
}
