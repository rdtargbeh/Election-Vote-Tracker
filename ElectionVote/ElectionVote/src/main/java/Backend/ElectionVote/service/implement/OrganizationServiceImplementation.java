package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.OrganizationCreateRequest;
import Backend.ElectionVote.dto.OrganizationDto;
import Backend.ElectionVote.dto.OrganizationUpdateRequest;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.Party;
import Backend.ElectionVote.mapper.OrganizationMapper;
import Backend.ElectionVote.repository.OrganizationRepository;
import Backend.ElectionVote.repository.PartyRepository;
import Backend.ElectionVote.service.OrganizationService;
import Backend.ElectionVote.uility.OrganizationSearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OrganizationServiceImplementation implements OrganizationService {

    @Autowired
    private  OrganizationRepository organizationRepository;
    @Autowired
    private  PartyRepository partyRepository;

    private final OrganizationMapper mapper = new OrganizationMapper();


    @Override
    public OrganizationDto create(OrganizationCreateRequest req) {
        // subdomain unique if provided
        if (req.getSubdomain() != null && !req.getSubdomain().isBlank()) {
            if (organizationRepository.existsBySubdomainIgnoreCase(req.getSubdomain())) {
                throw new IllegalArgumentException("Subdomain already in use");
            }
        }

        Organization org = mapper.toEntity(req);

        // attach party if provided
        if (req.getPartyId() != null) {
            Party p = partyRepository.findById(req.getPartyId())
                    .orElseThrow(() -> new NoSuchElementException("Party not found"));
            org.setParty(p);
        }

        Organization saved = organizationRepository.save(org);
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrganizationDto> get(UUID orgId) {
        return organizationRepository.findById(orgId).map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrganizationDto> getBySubdomain(String subdomain) {
        if (subdomain == null || subdomain.isBlank()) return Optional.empty();
        return organizationRepository.findBySubdomainIgnoreCase(subdomain).map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationDto> search(OrganizationSearchRequest req, Pageable pageable) {
        Page<Organization> page = organizationRepository.search(req.getQ(), req.getActive(), req.getType(), pageable);
        return page.map(mapper::toDTO);
    }

    @Override
    public OrganizationDto update(UUID orgId, OrganizationUpdateRequest req) {
        Organization org = organizationRepository.findById(orgId).orElseThrow(() -> new NoSuchElementException("Organization not found"));

        // subdomain uniqueness when changed
        if (req.getSubdomain() != null) {
            String sub = req.getSubdomain();
            if (sub != null && !sub.isBlank()) {
                if (organizationRepository.existsBySubdomainIgnoreCaseAndOrgIdNot(sub, orgId)) {
                    throw new IllegalArgumentException("Subdomain already in use");
                }
            }
        }

        // apply scalars
        mapper.apply(req, org);

        // handle party relation changes
        if (req.getPartyId() != null) {
            if (req.getPartyId() == null) {
                org.setParty(null);
            } else {
                Party p = partyRepository.findById(req.getPartyId())
                        .orElseThrow(() -> new NoSuchElementException("Party not found"));
                org.setParty(p);
            }
        }

        return mapper.toDTO(org);
    }

    @Override
    public void setActive(UUID orgId, boolean active) {
        Organization org = organizationRepository.findById(orgId).orElseThrow(() -> new NoSuchElementException("Organization not found"));
        org.setActive(active);
    }

    @Override
    public void assignParty(UUID orgId, UUID partyId) {
        Organization org = organizationRepository.findById(orgId).orElseThrow(() -> new NoSuchElementException("Organization not found"));
        if (partyId == null) {
            org.setParty(null);
        } else {
            Party p = partyRepository.findById(partyId).orElseThrow(() -> new NoSuchElementException("Party not found"));
            org.setParty(p);
        }
    }
}
