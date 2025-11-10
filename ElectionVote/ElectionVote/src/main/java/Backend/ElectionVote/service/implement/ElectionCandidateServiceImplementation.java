package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.ElectionCandidateCreateRequest;
import Backend.ElectionVote.dto.ElectionCandidateDto;
import Backend.ElectionVote.dto.ElectionCandidateUpdateRequest;
import Backend.ElectionVote.entity.Candidate;
import Backend.ElectionVote.entity.Election;
import Backend.ElectionVote.entity.ElectionCandidate;
import Backend.ElectionVote.entity.PollingCenter;
import Backend.ElectionVote.mapper.ElectionCandidateMapper;
import Backend.ElectionVote.repository.CandidateRepository;
import Backend.ElectionVote.repository.ElectionCandidateRepository;
import Backend.ElectionVote.repository.ElectionRepository;
import Backend.ElectionVote.repository.PollingCenterRepository;
import Backend.ElectionVote.service.ElectionCandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ElectionCandidateServiceImplementation implements ElectionCandidateService {

    private final ElectionCandidateRepository electionCandidateRepository;
    private final ElectionRepository electionRepo;
    private final CandidateRepository candidateRepository;
    private final PollingCenterRepository pollingCenterRepository;
    private final ElectionCandidateMapper mapper = new ElectionCandidateMapper();

    @Override
    public ElectionCandidateDto create(ElectionCandidateCreateRequest req) {
        if (electionCandidateRepository.existsByElection_ElectionIdAndCandidate_CandidateId(req.getElectionId(), req.getCandidateId())) {
            throw new ResponseStatusException(CONFLICT,
                    "Candidate already registered for this election");
        }

        Election election = electionRepo.findById(req.getElectionId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Election not found"));
        Candidate candidate = candidateRepository.findById(req.getCandidateId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Candidate not found"));
        PollingCenter center = (req.getCenterId() != null)
                ? pollingCenterRepository.findById(req.getCenterId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling center not found"))
                : null;

        ElectionCandidate saved = electionCandidateRepository.save(mapper.toEntity(req, election, candidate, center));
        return mapper.toDTO(saved);
    }

    @Override
    public ElectionCandidateDto update(UUID id, ElectionCandidateUpdateRequest req) {
        ElectionCandidate ec = electionCandidateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Election candidate not found"));
        PollingCenter newCenter = (req.getCenterId() != null)
                ? pollingCenterRepository.findById(req.getCenterId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling center not found"))
                : null;
        mapper.apply(req, ec, newCenter);
        return mapper.toDTO(electionCandidateRepository.save(ec));
    }

    @Override
    public void delete(UUID id) {
        if (!electionCandidateRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Election candidate not found");
        }
        electionCandidateRepository.deleteById(id);
    }

    @Override
    public ElectionCandidateDto get(UUID id) {
        return electionCandidateRepository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Election candidate not found"));
    }

    @Override
    public Page<ElectionCandidateDto> getAll(Pageable pageable) {
        return electionCandidateRepository.findAll(pageable).map(mapper::toDTO);
    }
}
