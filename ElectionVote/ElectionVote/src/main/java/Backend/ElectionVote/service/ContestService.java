package Backend.ElectionVote.service;



import Backend.ElectionVote.dto.ContestCreateRequest;
import Backend.ElectionVote.dto.ContestDto;
import Backend.ElectionVote.dto.ContestUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface ContestService {

    ContestDto create(ContestCreateRequest req);

    ContestDto update(UUID contestId, ContestUpdateRequest req);

    ContestDto getContest(UUID contestId);

    List<ContestDto> listByElection(UUID electionId);

    void deleteContest(UUID contestId);


}