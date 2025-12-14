package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.NecResultHistoryDto;
import Backend.ElectionVote.mapper.NecResultHistoryMapper;
import Backend.ElectionVote.repository.NecResultHistoryRepository;
import Backend.ElectionVote.service.NecResultHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NecResultHistoryServiceImpl implements NecResultHistoryService {

    private final NecResultHistoryRepository repository;
    private final NecResultHistoryMapper mapper;

    @Override
    public List<NecResultHistoryDto> listByResultId(UUID resultId) {
        return repository.findByResultIdOrderByChangedAtDesc(resultId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<NecResultHistoryDto> listByElectionId(UUID electionId) {
        return repository.findByElectionIdOrderByChangedAtDesc(electionId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}