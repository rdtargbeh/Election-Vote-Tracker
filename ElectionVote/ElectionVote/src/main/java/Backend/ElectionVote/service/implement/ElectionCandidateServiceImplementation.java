package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.ElectionCandidateCreateRequest;
import Backend.ElectionVote.dto.ElectionCandidateDto;
import Backend.ElectionVote.dto.ElectionCandidateUpdateRequest;
import Backend.ElectionVote.entity.Candidate;
import Backend.ElectionVote.entity.Election;
import Backend.ElectionVote.entity.ElectionCandidate;
import Backend.ElectionVote.entity.PollingCenter;
import Backend.ElectionVote.mapper.ElectionCandidateMapper;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.service.ElectionCandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class ElectionCandidateServiceImplementation implements ElectionCandidateService {

    private final ElectionCandidateRepository electionCandidateRepository;
    private final ElectionRepository electionRepo;
    private final CandidateRepository candidateRepository;
    private final PollingCenterRepository pollingCenterRepository;
    private final ElectionPartyRepository electionPartyRepository;

    private final ElectionCandidateMapper mapper;



    @Override
    public ElectionCandidateDto create(ElectionCandidateCreateRequest req) {
        // 1) Prevent duplicate candidate in the same election
        if (electionCandidateRepository
                .existsByElection_ElectionIdAndCandidate_CandidateId(req.getElectionId(), req.getCandidateId())) {

            throw new ResponseStatusException(
                    CONFLICT,
                    "Candidate is already registered for this election"
            );
        }

        // 2) Load election and candidate
        Election election = electionRepo.findById(req.getElectionId())
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Election not found"
                ));

        Candidate candidate = candidateRepository.findById(req.getCandidateId())
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Candidate not found"
                ));

        // 3) Optional polling center (null = nationwide/district-scoped)
        PollingCenter center = (req.getCenterId() != null)
                ? pollingCenterRepository.findById(req.getCenterId())
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Polling center not found"
                ))
                : null;

        // 4) Enforce party vs. independent rules at service level
        if (candidate.isIndependent()) {
            // Independent candidate must NOT have a party_id
            if (candidate.getParty() != null) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "Independent candidate must not have a party assigned"
                );
            }
            // No election_party check needed for independents
        } else {
            // Party-based candidate must have a party
            if (candidate.getParty() == null || candidate.getParty().getPartyId() == null) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "Candidate must have a party assigned before being registered to an election"
                );
            }

            UUID partyId = candidate.getParty().getPartyId();

            // Ensure that party is registered for this election (election_party table)
            boolean partyRegistered = electionPartyRepository
                    .existsByElection_ElectionIdAndParty_PartyId(election.getElectionId(), partyId);

            if (!partyRegistered) {
                throw new ResponseStatusException(
                        CONFLICT,
                        "Party " + candidate.getParty().getAbbreviation()
                                + " is not registered for election '" + election.getElectionName() + "'"
                );
            }
        }
        // 5) Persist election-candidate
        ElectionCandidate saved =
                electionCandidateRepository.save(mapper.toEntity(req, election, candidate, center));

        return mapper.toDTO(saved);   // use toDTO or toDto based on your mapper signature
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
