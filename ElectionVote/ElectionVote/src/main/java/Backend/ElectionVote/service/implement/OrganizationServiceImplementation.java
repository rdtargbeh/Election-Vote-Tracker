package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.OrganizationCreateRequest;
import Backend.ElectionVote.dto.OrganizationDto;
import Backend.ElectionVote.dto.OrganizationUpdateRequest;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.Party;
import Backend.ElectionVote.enums.OrganizationType;
import Backend.ElectionVote.mapper.OrganizationMapper;
import Backend.ElectionVote.repository.OrganizationRepository;
import Backend.ElectionVote.repository.PartyRepository;
import Backend.ElectionVote.service.OrganizationService;
import Backend.ElectionVote.utility.OrganizationSearchRequest;
import Backend.ElectionVote.utility.QueryUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationServiceImplementation implements OrganizationService {

    @Autowired
    private  OrganizationRepository organizationRepository;
    @Autowired
    private  PartyRepository partyRepository;

    private final OrganizationMapper mapper = new OrganizationMapper();


    @Override
    @Transactional // write Tx
    public OrganizationDto create(OrganizationCreateRequest req) {
        // normalize subdomain
        String sub = normalizeSubdomain(req.getSubdomain());
        if (sub != null) {
            // case-insensitive uniqueness
            if (organizationRepository.existsBySubdomainIgnoreCase(sub)) {
                throw new IllegalArgumentException("Subdomain already in use");
            }
        }
        // Map DTO → entity
        Organization org = mapper.toEntity(req);
        org.setSubdomain(sub); // ensure normalized value is persisted

        // ✅ NEW: Organization has NO party FK; just save org
        Organization saved = organizationRepository.save(org);
        return mapper.toDTO(saved);
    }



    @Override
    @Transactional(readOnly = true)
    public Optional<OrganizationDto> get(UUID orgId) {
        return organizationRepository.findById(orgId).map(mapper::toDTO);
    }

    @Override
    public Optional<OrganizationDto> getBySubdomain(String subdomain) {
        String sub = normalizeSubdomain(subdomain);
        if (sub == null) return Optional.empty();
        return organizationRepository.findBySubdomainIgnoreCase(sub).map(mapper::toDTO);
    }


    @Override
    public Page<OrganizationDto> search(OrganizationSearchRequest req, Pageable pageable) {
        String q = QueryUtils.normalize(req != null ? req.getQ() : null);
        Boolean active = (req != null) ? req.getActive() : null;
        OrganizationType type = (req != null) ? req.getType() : null; // <-- enum, not String
        return organizationRepository.search(q, active, type, pageable)
                .map(mapper::toDTO);
    }


    @Override
    @Transactional // write Tx
    public OrganizationDto update(UUID orgId, OrganizationUpdateRequest req) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        // subdomain: only validate when the client sent a value (including blank)
        if (req.getSubdomain() != null) {
            String sub = normalizeSubdomain(req.getSubdomain());
            if (sub != null) {
                if (organizationRepository.existsBySubdomainIgnoreCaseAndOrgIdNot(sub, orgId)) {
                    throw new IllegalArgumentException("Subdomain already in use");
                }
            }
            org.setSubdomain(sub); // may be null to clear
        }
        // apply scalar changes via mapper (safe fields only)
        mapper.apply(req, org);

        Organization saved = organizationRepository.save(org);
        return mapper.toDTO(saved);
    }


    @Override
    @Transactional
    public void setActive(UUID orgId, boolean active) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));
        org.setActive(active);
    }



    private String normalizeSubdomain(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase();
        return s.isBlank() ? null : s;
    }


}
