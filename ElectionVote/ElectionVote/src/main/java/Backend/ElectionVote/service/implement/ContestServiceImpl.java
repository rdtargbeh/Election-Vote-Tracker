package Backend.ElectionVote.service.implement;


import Backend.ElectionVote.dto.ContestCreateRequest;
import Backend.ElectionVote.dto.ContestDto;
import Backend.ElectionVote.dto.ContestUpdateRequest;
import Backend.ElectionVote.entity.Contest;
import Backend.ElectionVote.entity.ContestOption;
import Backend.ElectionVote.enums.*;
import Backend.ElectionVote.mapper.ContestMapper;
import Backend.ElectionVote.repository.ContestOptionRepository;
import Backend.ElectionVote.repository.ContestRepository;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.service.ContestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class ContestServiceImpl implements ContestService {

    private final ContestRepository contestRepo;
    private final ContestOptionRepository contestOptionRepository;
    private final AuthorizationService authz;
    private final ContestMapper contestMapper = new ContestMapper();


    @Override
    public ContestDto create(ContestCreateRequest req) {
        authz.requireNecAdminOrPlatformAdmin();

        Contest c = contestMapper.toEntity(req);

        // ✅ renamed method
        normalizeCandidateVotesToOptionId(c);

        Contest saved = contestRepo.save(c);
        return contestMapper.toDto(saved);
    }


//    @Override
//    public ContestDto create(ContestCreateRequest req) {
//        authz.requireNecAdminOrPlatformAdmin();
//
//        Contest c = contestMapper.toEntity(req);
//        normalizeAndValidate(c);
//
//        Contest saved = contestRepo.save(c);
//        return contestMapper.toDto(saved);
//    }


    @Override
    public ContestDto update(UUID contestId, ContestUpdateRequest req) {
        authz.requireNecAdminOrPlatformAdmin();

        Contest c = contestRepo.findById(contestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contest not found"));

        // LOCK protection stays unchanged
        if (c.getStatus() == ContestStatus.LOCKED) {
            if (req.getElectionId() != null
                    || req.getCategory() != null
                    || req.getScopeType() != null
                    || req.getCountyId() != null
                    || req.getDistrictId() != null
                    || req.getVoteMethod() != null
                    || req.getSeats() != null
                    || req.getMaxSelections() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Contest is LOCKED. Structural fields cannot be modified.");
            }
        }

        if (req.getElectionId() != null && c.getStatus() != ContestStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change electionId unless contest is DRAFT.");
        }

        contestMapper.apply(req, c);

        // ✅ renamed method
        normalizeCandidateVotesToOptionId(c);

        Contest saved = contestRepo.save(c);
        return contestMapper.toDto(saved);
    }


//    @Override
//    public ContestDto update(UUID contestId, ContestUpdateRequest req) {
//        authz.requireNecAdminOrPlatformAdmin();
//
//        Contest c = contestRepo.findById(contestId)
//                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contest not found"));
//
//        // If contest is LOCKED, block structural changes (keep it simple & safe)
//        if (c.getStatus() == ContestStatus.LOCKED) {
//            if (req.getElectionId() != null
//                    || req.getCategory() != null
//                    || req.getScopeType() != null
//                    || req.getCountyId() != null
//                    || req.getDistrictId() != null
//                    || req.getVoteMethod() != null
//                    || req.getSeats() != null
//                    || req.getMaxSelections() != null) {
//                throw new ResponseStatusException(HttpStatus.CONFLICT,
//                        "Contest is LOCKED. Structural fields cannot be modified.");
//            }
//        }
//
//        // Don’t allow electionId move after publish/lock (best practice)
//        if (req.getElectionId() != null && c.getStatus() != ContestStatus.DRAFT) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT,
//                    "Cannot change electionId unless contest is DRAFT.");
//        }
//
//        contestMapper.apply(req, c);
//        normalizeAndValidate(c);
//
//        Contest saved = contestRepo.save(c);
//        return contestMapper.toDto(saved);
//    }

    @Override
    @Transactional(readOnly = true)
    public ContestDto getContest(UUID contestId) {
        return contestRepo.findById(contestId)
                .map(contestMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contest not found"));
    }


    @Override
    @Transactional(readOnly = true)
    public List<ContestDto> listByElection(UUID electionId) {
        return contestRepo.findByElectionId(electionId)
                .stream()
                .map(contestMapper::toDto)
                .collect(Collectors.toList());
    }


    @Override
    public void deleteContest(UUID contestId) {
        authz.requireNecAdminOrPlatformAdmin();
        if (!contestRepo.existsById(contestId)) {
            throw new ResponseStatusException(NOT_FOUND, "Contest not found");
        }
        contestRepo.deleteById(contestId);
    }

    /**
     * Normalizes and validates Contest configuration so that
     * vote submissions can be safely aggregated via contest_option.option_id.
     *
     * This does NOT create options — it only enforces correctness
     * before options & submissions rely on the contest definition.
     */
    private void normalizeCandidateVotesToOptionId(Contest c) {

        if (c == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contest body is required.");
        }

        if (c.getElectionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "electionId is required.");
        }

        if (c.getContestName() == null || c.getContestName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contestName is required.");
        }
        c.setContestName(c.getContestName().trim());

        // defaults (mirror DB + entity PrePersist)
        if (c.getCategory() == null) c.setCategory(ContestCategory.OTHER);
        if (c.getScopeType() == null) c.setScopeType(ContestScopeType.NATIONAL);
        if (c.getVoteMethod() == null) c.setVoteMethod(ContestVoteMethod.SINGLE_CHOICE);
        if (c.getStatus() == null) c.setStatus(ContestStatus.DRAFT);

        if (c.getSeats() <= 0) c.setSeats(1);
        if (c.getMaxSelections() <= 0) c.setMaxSelections(1);
        if (c.getMaxSelections() < c.getSeats()) {
            c.setMaxSelections(c.getSeats());
        }

        // scope integrity (must match DB constraint)
        if (c.getScopeType() == ContestScopeType.NATIONAL) {
            c.setCountyId(null);
            c.setDistrictId(null);
        }
        else if (c.getScopeType() == ContestScopeType.COUNTY) {
            if (c.getCountyId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "countyId is required for COUNTY scope."
                );
            }
            c.setDistrictId(null);
        }
        else if (c.getScopeType() == ContestScopeType.DISTRICT) {
            if (c.getDistrictId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "districtId is required for DISTRICT scope."
                );
            }
            // countyId may be inferred from district — leave as-is
        }

        // sanity guards (optional but safe)
        if (c.getSeats() > 99) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seats is too large.");
        }
        if (c.getMaxSelections() > 99) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxSelections is too large.");
        }
    }


}