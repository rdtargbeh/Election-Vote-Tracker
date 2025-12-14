package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.NecResultHistoryDto;

import java.util.List;
import java.util.UUID;

public interface NecResultHistoryService {
    List<NecResultHistoryDto> listByResultId(UUID resultId);
    List<NecResultHistoryDto> listByElectionId(UUID electionId);
}
