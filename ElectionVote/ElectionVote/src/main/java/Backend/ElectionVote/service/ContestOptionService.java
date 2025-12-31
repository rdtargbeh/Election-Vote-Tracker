package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.ContestOptionDto;

import java.util.List;
import java.util.UUID;

public interface ContestOptionService {
    ContestOptionDto createOption(ContestOptionDto dto);
    ContestOptionDto updateOption(UUID optionId, ContestOptionDto dto);
    ContestOptionDto getOption(UUID optionId);
    List<ContestOptionDto> listByContest(UUID contestId, boolean onlyActive);
    void deleteOption(UUID optionId);
    List<ContestOptionDto> findByCandidateId(UUID candidateId);
}