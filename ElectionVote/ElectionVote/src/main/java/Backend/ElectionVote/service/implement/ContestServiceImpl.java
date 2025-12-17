package Backend.ElectionVote.service.implement;


import Backend.ElectionVote.dto.ContestDto;
import Backend.ElectionVote.entity.Contest;
import Backend.ElectionVote.repository.ContestRepository;
import Backend.ElectionVote.service.ContestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class ContestServiceImpl implements ContestService {

    private final ContestRepository contestRepo;

    @Override
    public ContestDto createContest(ContestDto dto) {
        Contest c = new Contest();
        c.setContestId(dto.getContestId() == null ? UUID.randomUUID() : dto.getContestId());
        c.setElectionId(dto.getElectionId());
        c.setContestName(dto.getContestName());
        c.setContestType(dto.getContestType() == null ? "SINGLE_CHOICE" : dto.getContestType());
        c.setDescription(dto.getDescription());
        c.setIsActive(dto.getIsActive() == null ? Boolean.TRUE : dto.getIsActive());
        Contest saved = contestRepo.save(c);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContestDto getContest(UUID contestId) {
        return contestRepo.findById(contestId)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contest not found"));
    }

    @Override
    public ContestDto updateContest(UUID contestId, ContestDto dto) {
        Contest c = contestRepo.findById(contestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contest not found"));

        if (dto.getContestName() != null) c.setContestName(dto.getContestName());
        if (dto.getContestType() != null) c.setContestType(dto.getContestType());
        if (dto.getDescription() != null) c.setDescription(dto.getDescription());
        if (dto.getIsActive() != null) c.setIsActive(dto.getIsActive());
        // electionId generally shouldn't be changed, but allow if provided
        if (dto.getElectionId() != null) c.setElectionId(dto.getElectionId());

        Contest saved = contestRepo.save(c);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContestDto> listByElection(UUID electionId) {
        List<Contest> items = contestRepo.findByElectionId(electionId);
        return items.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public void deleteContest(UUID contestId) {
        if (!contestRepo.existsById(contestId)) {
            throw new ResponseStatusException(NOT_FOUND, "Contest not found");
        }
        contestRepo.deleteById(contestId);
    }

    private ContestDto toDto(Contest c) {
        if (c == null) return null;
        ContestDto d = new ContestDto();
        d.setContestId(c.getContestId());
        d.setElectionId(c.getElectionId());
        d.setContestName(c.getContestName());
        d.setContestType(c.getContestType());
        d.setDescription(c.getDescription());
        d.setIsActive(c.getIsActive());
        d.setDateCreated(c.getDateCreated());
        return d;
    }
}