package Backend.ElectionVote.service;


import Backend.ElectionVote.dto.VoteSubmissionRankingDto;
import Backend.ElectionVote.dto.VoteSubmissionRankingUpsertRequest;
import Backend.ElectionVote.entity.VoteSubmissionRanking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VoteSubmissionRankingService  {

   VoteSubmissionRankingDto createOrUpdateRanking(VoteSubmissionRankingDto dto);

   VoteSubmissionRankingDto getById(UUID svrId);

   List<VoteSubmissionRankingDto> getBySubmission(UUID submissionId);

   List<VoteSubmissionRankingDto> getByContest(UUID contestId);

   void deleteById(UUID svrId);

   int backfillFromVerifiedSubmissionsForElection(UUID electionId);


}
