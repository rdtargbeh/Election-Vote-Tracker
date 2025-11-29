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

import static org.springframework.http.HttpStatus.*;

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

        // Determine independent flag explicitly
        boolean isIndependent = Boolean.TRUE.equals(req.getIndependent());
        Party party = null;

        if (isIndependent) {
            // Independent candidate MUST NOT have a party
            if (req.getPartyId() != null) {
                throw new ResponseStatusException(
                        BAD_REQUEST, "Independent candidates cannot belong to a party"
                );
            }
            // Duplicate guard for independents
            if (candidateRepository.existsByFullNameIgnoreCaseAndPartyIsNull(req.getFullName())) {
                throw new ResponseStatusException(
                        CONFLICT, "Independent candidate with this name already exists"
                );
            }
        } else {
            // Party candidate
            if (req.getPartyId() == null) {
                throw new ResponseStatusException(
                        BAD_REQUEST, "partyId is required unless candidate is independent"
                );
            }
            party = partyRepository.findById(req.getPartyId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Party not found"));

            // Duplicate guard within party
            if (candidateRepository.existsByFullNameIgnoreCaseAndParty_PartyId(
                    req.getFullName(), party.getPartyId())) {

                throw new ResponseStatusException(
                        CONFLICT, "Candidate already exists in this party"
                );
            }
        }
        Candidate saved = candidateRepository.save(mapper.toEntity(req, party));
        return mapper.toDTO(saved);
    }



    @Override
    public CandidateDto update(UUID id, CandidateUpdateRequest req) {

        Candidate entity = candidateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Candidate not found"));

        boolean newIndependentFlag = (req.getIndependent() != null)
                ? req.getIndependent()
                : entity.isIndependent();

        Party newParty = null;

        // ─────────────────────────────────────────────
        // A) Candidate becomes INDEPENDENT
        // ─────────────────────────────────────────────
        if (newIndependentFlag) {
            // Cannot set a party while independent
            if (req.getPartyId() != null) {
                throw new ResponseStatusException(
                        BAD_REQUEST, "Independent candidates cannot belong to a party"
                );
            }
            // Duplicate guard for rename among independents
            if (req.getFullName() != null) {
                boolean dup = candidateRepository.existsByFullNameIgnoreCaseAndPartyIsNull(req.getFullName())
                        && !req.getFullName().equalsIgnoreCase(entity.getFullName());

                if (dup) {
                    throw new ResponseStatusException(
                            CONFLICT, "Independent candidate with this name already exists"
                    );
                }
            }
            newParty = null; // Force party = null
        }

        // ─────────────────────────────────────────────
        // B) Candidate remains or becomes PARTY candidate
        // ─────────────────────────────────────────────
        else {
            if (req.getPartyId() != null) {
                newParty = partyRepository.findById(req.getPartyId())
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Party not found"));
            } else {
                // If not independent, must have a party
                if (entity.getParty() == null) {
                    throw new ResponseStatusException(
                            BAD_REQUEST, "partyId is required for non-independent candidates"
                    );
                }
                newParty = entity.getParty(); // Keep existing party
            }
            // Duplicate guard inside the party
            String newName = req.getFullName() != null ? req.getFullName() : entity.getFullName();

            boolean dup = candidateRepository.existsByFullNameIgnoreCaseAndParty_PartyId(
                    newName, newParty.getPartyId())
                    && !(entity.getParty() != null
                    && newParty.getPartyId().equals(entity.getParty().getPartyId())
                    && newName.equalsIgnoreCase(entity.getFullName()));

            if (dup) {
                throw new ResponseStatusException(
                        CONFLICT, "Candidate with this name already exists in the target party"
                );
            }
        }

        // Apply changes
        mapper.apply(req, entity, newParty);
        Candidate saved = candidateRepository.save(entity);

        return mapper.toDTO(saved);
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
