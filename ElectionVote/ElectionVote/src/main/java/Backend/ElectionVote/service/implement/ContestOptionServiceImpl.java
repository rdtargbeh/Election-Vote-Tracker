package Backend.ElectionVote.service.implement;


import Backend.ElectionVote.dto.ContestOptionDto;
import Backend.ElectionVote.entity.Contest;
import Backend.ElectionVote.entity.ContestOption;
import Backend.ElectionVote.repository.ContestOptionRepository;
import Backend.ElectionVote.repository.ContestRepository;
import Backend.ElectionVote.service.ContestOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Service implementing ContestOption CRUD and helpers.
 *
 * Behavior/decisions:
 * - createOption: ensures referenced contest exists, sets option_order automatically if not provided.
 * - updateOption: allows updates to label, candidateId, isActive, optionOrder.
 * - deleteOption: removes option by id.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ContestOptionServiceImpl implements ContestOptionService {

    private final ContestOptionRepository optionRepo;
    private final ContestRepository contestRepo;

    @Override
    public ContestOptionDto createOption(ContestOptionDto dto) {
        // Validate contest exists
        Contest contest = contestRepo.findById(dto.getContestId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contest not found: " + dto.getContestId()));

        ContestOption opt = new ContestOption();
        opt.setOptionId(dto.getOptionId() == null ? UUID.randomUUID() : dto.getOptionId());
        opt.setContestId(contest.getContestId());
        opt.setCandidateId(dto.getCandidateId());
        opt.setOptionLabel(dto.getOptionLabel());
        // compute optionOrder if not provided: max + 1
        Integer providedOrder = dto.getOptionOrder();
        if (providedOrder == null) {
            Integer max = optionRepo.findMaxOptionOrderForContest(contest.getContestId());
            opt.setOptionOrder((max == null ? 0 : max) + 1);
        } else {
            opt.setOptionOrder(providedOrder);
        }
        opt.setIsActive(dto.getIsActive() == null ? Boolean.TRUE : dto.getIsActive());
        opt.setDateCreated(LocalDateTime.now());

        ContestOption saved = optionRepo.save(opt);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContestOptionDto getOption(UUID optionId) {
        ContestOption opt = optionRepo.findById(optionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Option not found: " + optionId));
        return toDto(opt);
    }

    @Override
    public ContestOptionDto updateOption(UUID optionId, ContestOptionDto dto) {
        ContestOption opt = optionRepo.findById(optionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Option not found: " + optionId));

        if (dto.getOptionLabel() != null) opt.setOptionLabel(dto.getOptionLabel());
        if (dto.getCandidateId() != null) opt.setCandidateId(dto.getCandidateId());
        if (dto.getOptionOrder() != null) opt.setOptionOrder(dto.getOptionOrder());
        if (dto.getIsActive() != null) opt.setIsActive(dto.getIsActive());

        ContestOption saved = optionRepo.save(opt);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContestOptionDto> listByContest(UUID contestId, boolean onlyActive) {
        // ensure contest exists
        contestRepo.findById(contestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contest not found: " + contestId));
        List<ContestOption> items = onlyActive ? optionRepo.findByContestIdAndIsActiveTrueOrderByOptionOrderAsc(contestId)
                : optionRepo.findByContestIdOrderByOptionOrderAsc(contestId);
        return items.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public void deleteOption(UUID optionId) {
        if (!optionRepo.existsById(optionId)) {
            throw new ResponseStatusException(NOT_FOUND, "Option not found: " + optionId);
        }
        optionRepo.deleteById(optionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContestOptionDto> findByCandidateId(UUID candidateId) {
        List<ContestOption> items = optionRepo.findByCandidateId(candidateId);
        return items.stream().map(this::toDto).collect(Collectors.toList());
    }

    private ContestOptionDto toDto(ContestOption c) {
        if (c == null) return null;
        ContestOptionDto d = new ContestOptionDto();
        d.setOptionId(c.getOptionId());
        d.setContestId(c.getContestId());
        d.setCandidateId(c.getCandidateId());
        d.setOptionLabel(c.getOptionLabel());
        d.setOptionOrder(c.getOptionOrder());
        d.setIsActive(c.getIsActive());
        d.setDateCreated(c.getDateCreated());
        return d;
    }
}