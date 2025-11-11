package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.CandidateCreateRequest;
import Backend.ElectionVote.dto.CandidateDto;
import Backend.ElectionVote.dto.CandidateUpdateRequest;
import Backend.ElectionVote.entity.Candidate;
import Backend.ElectionVote.entity.Party;
import Backend.ElectionVote.mapper.CandidateMapper;
import Backend.ElectionVote.repository.CandidateRepository;
import Backend.ElectionVote.repository.PartyRepository;
import Backend.ElectionVote.service.CandidateService;
import Backend.ElectionVote.utility.CandidateSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CandidateServiceImplementation implements CandidateService {

    @Autowired
    private CandidateRepository candidateRepository;
    @Autowired
    private PartyRepository partyRepository;
    private final CandidateMapper mapper = new CandidateMapper();

    @Override
    public CandidateDto create(CandidateCreateRequest req) {
        Party party = null;
        if (req.getPartyId() != null) {
            party = partyRepository.findById(req.getPartyId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Party not found"));
            if (candidateRepository.existsByFullNameIgnoreCaseAndParty_PartyId(req.getFullName(), party.getPartyId())) {
                throw new ResponseStatusException(CONFLICT, "Candidate already exists in this party");
            }
        } else {
            if (candidateRepository.existsByFullNameIgnoreCaseAndPartyIsNull(req.getFullName())) {
                throw new ResponseStatusException(CONFLICT, "Independent candidate with this name already exists");
            }
        }

        Candidate saved = candidateRepository.save(mapper.toEntity(req, party));
        return mapper.toDTO(saved);
    }

    @Override
    public CandidateDto update(UUID id, CandidateUpdateRequest req) {
        Candidate entity = candidateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Candidate not found"));

        Party newParty = null;
        if (req.getPartyId() != null) {
            newParty = partyRepository.findById(req.getPartyId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Party not found"));
            // Duplicate guard when moving to a party or renaming within same party
            String name = req.getFullName() != null ? req.getFullName() : entity.getFullName();
            UUID pid = newParty.getPartyId();
            boolean dup = candidateRepository.existsByFullNameIgnoreCaseAndParty_PartyId(name, pid)
                    && !(name.equalsIgnoreCase(entity.getFullName())
                    && entity.getParty() != null
                    && pid.equals(entity.getParty().getPartyId()));
            if (dup) {
                throw new ResponseStatusException(CONFLICT, "Candidate with this name already exists in the target party");
            }
        } else if (req.getFullName() != null && entity.getParty() == null) {
            // Independent rename duplicate guard
            boolean dupInd = candidateRepository.existsByFullNameIgnoreCaseAndPartyIsNull(req.getFullName())
                    && !req.getFullName().equalsIgnoreCase(entity.getFullName());
            if (dupInd) throw new ResponseStatusException(CONFLICT, "Independent candidate with this name already exists");
        }

        mapper.apply(req, entity, newParty);
        return mapper.toDTO(candidateRepository.save(entity));
    }

    @Override
    public void delete(UUID id) {
        if (!candidateRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Candidate not found");
        }
        candidateRepository.deleteById(id);
    }

    @Override
    public CandidateDto get(UUID id) {
        return candidateRepository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Candidate not found"));
    }

    @Override
    public Page<CandidateDto> search(String q, String position, UUID partyId, Boolean active, Pageable pageable) {
        Specification<Candidate> spec = Specification
                .where(CandidateSpecs.nameContains(q))
                .and(CandidateSpecs.positionContains(position))
                .and(CandidateSpecs.partyEquals(partyId))
                .and(CandidateSpecs.activeEquals(active));
        return candidateRepository.findAll(spec, pageable).map(mapper::toDTO);
    }

}
