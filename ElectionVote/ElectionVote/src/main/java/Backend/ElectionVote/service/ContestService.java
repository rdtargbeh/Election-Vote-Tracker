package Backend.ElectionVote.service;



import Backend.ElectionVote.dto.ContestDto;

import java.util.List;
import java.util.UUID;

public interface ContestService {
    ContestDto createContest(ContestDto dto);
    ContestDto updateContest(UUID contestId, ContestDto dto);
    ContestDto getContest(UUID contestId);
    List<ContestDto> listByElection(UUID electionId);
    void deleteContest(UUID contestId);
}