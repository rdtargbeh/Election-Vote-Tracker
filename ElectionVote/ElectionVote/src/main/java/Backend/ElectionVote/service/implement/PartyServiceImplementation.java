package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.PartyCreateRequest;
import Backend.ElectionVote.dto.PartyDto;
import Backend.ElectionVote.dto.PartyUpdateRequest;
import Backend.ElectionVote.entity.Party;
import Backend.ElectionVote.mapper.PartyMapper;
import Backend.ElectionVote.repository.OrganizationRepository;
import Backend.ElectionVote.repository.PartyRepository;
import Backend.ElectionVote.service.PartyService;
import Backend.ElectionVote.utility.PartySearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static Backend.ElectionVote.utility.QueryUtils.normalize;

@Service
@RequiredArgsConstructor
@Transactional
public class PartyServiceImplementation implements PartyService {

    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    private final PartyMapper mapper = new PartyMapper();

    @Override
    public PartyDto create(PartyCreateRequest req) {
        // friendly uniqueness checks
        if (partyRepository.existsByPartyNameIgnoreCase(req.getPartyName()))
            throw new IllegalArgumentException("Party name already exists");
        if (partyRepository.existsByAbbreviationIgnoreCase(req.getAbbreviation()))
            throw new IllegalArgumentException("Abbreviation already exists");

        Party entity = mapper.toEntity(req);
        try {
            return mapper.toDTO(partyRepository.save(entity));
        } catch (DataIntegrityViolationException e) {
            // race protection
            throw new IllegalArgumentException("Party name or abbreviation already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PartyDto> get(UUID partyId) {
        return partyRepository.findById(partyId).map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PartyDto> getByAbbreviation(String abbrev) {
        if (abbrev == null || abbrev.isBlank()) return Optional.empty();
        return partyRepository.findByAbbreviationIgnoreCase(abbrev).map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartyDto> search(PartySearchRequest req, Pageable pageable) {
        return partyRepository
                .search(normalize(req != null ? req.getQ() : null), pageable)
                .map(mapper::toDTO);
    }


    @Override
    public PartyDto update(UUID partyId, PartyUpdateRequest req) {
        Party p = partyRepository.findById(partyId)
                .orElseThrow(() -> new NoSuchElementException("Party not found"));

        // uniqueness if changed
        if (req.getPartyName() != null &&
                partyRepository.existsByPartyNameIgnoreCaseAndPartyIdNot(req.getPartyName(), partyId)) {
            throw new IllegalArgumentException("Party name already exists");
        }
        if (req.getAbbreviation() != null &&
                partyRepository.existsByAbbreviationIgnoreCaseAndPartyIdNot(req.getAbbreviation(), partyId)) {
            throw new IllegalArgumentException("Abbreviation already exists");
        }

        mapper.apply(req, p);
        return mapper.toDTO(p);
    }

    @Override
    public void delete(UUID partyId) {
        Party p = partyRepository.findById(partyId)
                .orElseThrow(() -> new NoSuchElementException("Party not found"));

        // Guard: prevent delete if any org references this party
        boolean inUse = organizationRepository.existsByParty_PartyId(partyId); // add this method in OrganizationRepository
        if (inUse) {
            throw new IllegalStateException("Cannot delete party that is referenced by organizations");
        }

        partyRepository.delete(p);
    }
}
