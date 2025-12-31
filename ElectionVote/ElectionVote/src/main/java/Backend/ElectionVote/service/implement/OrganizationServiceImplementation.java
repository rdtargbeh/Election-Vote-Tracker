package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.OrganizationBrandingUpdateRequest;
import Backend.ElectionVote.dto.OrganizationCreateRequest;
import Backend.ElectionVote.dto.OrganizationDto;
import Backend.ElectionVote.dto.OrganizationUpdateRequest;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.Party;
import Backend.ElectionVote.enums.OrganizationType;
import Backend.ElectionVote.mapper.OrganizationMapper;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.security.CurrentUserProvider;
import Backend.ElectionVote.service.AuditLogService;
import Backend.ElectionVote.service.OrganizationService;
import Backend.ElectionVote.utility.OrganizationSearchRequest;
import Backend.ElectionVote.utility.QueryUtils;
import Backend.ElectionVote.utility.SecurityUtils;
import Backend.ElectionVote.utility.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationServiceImplementation implements OrganizationService {


    private final OrganizationRepository organizationRepository;
    private final  PartyRepository partyRepository;
    private final UserSessionRepository userSessionRepository;
    private final OrgMembershipRepository orgMembershipRepository;
    private final AuditLogService auditLogService;
    private final AuthorizationService authz;

    private final OrganizationMapper mapper = new OrganizationMapper();

    private final CurrentUserProvider currentUserProvider;
    private final JdbcTemplate jdbc;



    @Override
    @Transactional
    public OrganizationDto create(OrganizationCreateRequest req) {

        authz.requirePlatformAdmin(); // Require System Admin

        // 1) Normalize subdomain
        String sub = normalizeSubdomain(req.getSubdomain());
        if (sub != null && organizationRepository.existsBySubdomainIgnoreCase(sub)) {
            throw new IllegalArgumentException("Subdomain already in use");
        }

        // 2) Map DTO → entity (scalars only)
        Organization org = mapper.toEntity(req);
        org.setSubdomain(sub);

        // 3) Attach Party if provided
        if (req.getPartyId() != null) {
            Party party = partyRepository.findById(req.getPartyId())
                    .orElseThrow(() -> new NoSuchElementException("Party not found: " + req.getPartyId()));
            org.setParty(party);
        } else {
            org.setParty(null);
        }

        // 4) Save organization + FLUSH so it exists in DB before any JDBC audit insert
        Organization saved = organizationRepository.save(org);
        organizationRepository.flush(); // ✅ critical

        // 5) Audit using the NEW org id (do NOT use context org id for create)
        UUID actor = currentUserProvider.currentUserId(); // best source
        auditLogService.logOrgCreate(
                saved.getOrgId(),
                actor,
                "organization",
                "Organization created: " + saved.getOrgName() + " (" + saved.getOrgId() + ")"
        );

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

        authz.requirePlatformAdmin(); // Require System Admin

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

        // Attach/clear party if provided
        if (req.getPartyId() != null) {
            Party party = partyRepository.findById(req.getPartyId())
                    .orElseThrow(() -> new NoSuchElementException("Party not found: " + req.getPartyId()));
            org.setParty(party);
        } else if (req.getPartyId() == null) {
            org.setParty(null); // explicit clear
        }

        Organization saved = organizationRepository.save(org);

        // Audit log
        try {
            UUID actor = currentUserProvider.currentUserId();
            String desc = "Organization updated: " + saved.getOrgName() + " (" + saved.getOrgId() + ")";
            jdbc.update("INSERT INTO audit_log (log_id, org_id, user_id, activity_type, entity_affected, action_description) VALUES (gen_random_uuid(), ?, ?, ?, ?, ?)",
                    new Object[]{ saved.getOrgId(), actor, "ORGANIZATION_UPDATE", "organization", desc });
        } catch (Exception ignored) {}


        return mapper.toDTO(saved);

    }


    @Override
    @Transactional
    public OrganizationDto updateBranding(UUID id, OrganizationBrandingUpdateRequest req) {

        // Resolve org
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        TenantContext ctx = TenantContext.get();
        if (ctx == null) {
            throw new AccessDeniedException("Tenant context missing");
        }

        UUID tenantOrgId = ctx.orgId().orElse(null);
        boolean isPlatformAdmin = ctx.isSystemAdmin();

        // ✅ Tenant admins may only edit THEIR OWN org
        if (!isPlatformAdmin) {
            if (tenantOrgId == null) {
                throw new AccessDeniedException("Tenant required");
            }
            if (!tenantOrgId.equals(id)) {
                throw new AccessDeniedException("Cannot edit another organization");
            }
        }

        // ✅ Branding fields only (safe for tenant admins)
        if (req.getLogoUrl() != null) {
            org.setLogoUrl(req.getLogoUrl());
        }
        if (req.getPrimaryColor() != null) {
            org.setPrimaryColor(req.getPrimaryColor());
        }
        if (req.getSubdomain() != null) {
            org.setSubdomain(req.getSubdomain());
        }

        Organization saved = organizationRepository.save(org);
        return mapper.toDTO(saved);
    }



    @Override
    @Transactional
    public void setActive(UUID orgId, boolean active) {

        authz.requirePlatformAdmin(); // Require System Admin

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        org.setActive(active);

        // When deactivating an org, disable all memberships for that org
        if (!active) {
            orgMembershipRepository.disableAllForOrg(orgId);   // custom repo method
            userSessionRepository.revokeAllForOrg(orgId);      // optional but nice
        }

        // Audit log
        try {
            UUID actor = currentUserProvider.currentUserId();
            String desc = "Organization " + (active ? "activated: " : "deactivated: ") + org.getOrgName() + " (" + org.getOrgId() + ")";
            jdbc.update("INSERT INTO audit_log (log_id, org_id, user_id, activity_type, entity_affected, action_description) VALUES (gen_random_uuid(), ?, ?, ?, ?, ?)",
                    new Object[]{ org.getOrgId(), actor, "ORGANIZATION_SET_ACTIVE", "organization", desc });
        } catch (Exception ignored) {}
    }


    private String normalizeSubdomain(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase();
        return s.isBlank() ? null : s;
    }


}
