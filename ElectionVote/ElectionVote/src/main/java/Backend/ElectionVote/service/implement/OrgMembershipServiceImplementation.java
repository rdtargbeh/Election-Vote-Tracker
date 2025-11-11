package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.MemberSearchRequest;
import Backend.ElectionVote.dto.MembershipCreateRequest;
import Backend.ElectionVote.dto.OrgMembershipDto;
import Backend.ElectionVote.entity.OrgMembership;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.SystemUser;
import Backend.ElectionVote.mapper.OrgMembershipMapper;
import Backend.ElectionVote.repository.OrgMembershipRepository;
import Backend.ElectionVote.repository.OrganizationRepository;
import Backend.ElectionVote.repository.SystemUserRepository;
import Backend.ElectionVote.service.OrgMembershipService;
import Backend.ElectionVote.utility.QueryUtils;
import Backend.ElectionVote.utility.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class OrgMembershipServiceImplementation implements OrgMembershipService {

    @Autowired
    private  OrgMembershipRepository memberships;
    @Autowired
    private  SystemUserRepository users;
    @Autowired
    private  OrganizationRepository orgs;

    private  final OrgMembershipMapper mapper = new OrgMembershipMapper();

    @Override
    @Transactional(readOnly = true)
    public Page<OrgMembershipDto> listInTenant(MemberSearchRequest req, Pageable pageable) {
        UUID orgId = requireTenant();
        return memberships.searchInOrg(
                orgId,
                QueryUtils.normalize(req != null ? req.getQ() : null),
                req != null ? req.getRoleName() : null,
                req != null ? req.getEnabled() : null,
                pageable
        ).map(mapper::toDTO);
    }


    @Override
    public OrgMembershipDto addMemberInTenant(MembershipCreateRequest req) {
        UUID orgId = requireTenant();
        Organization org = orgs.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        SystemUser user = users.findById(req.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (memberships.existsByOrganization_OrgIdAndUser_UserId(orgId, user.getUserId())) {
            throw new IllegalArgumentException("User is already a member of this organization");
        }

        OrgMembership m = new OrgMembership();
        m.setOrganization(org);
        m.setUser(user);
        m.setRoleName(req.getRoleName());
        m.setEnabled(true);

        OrgMembership saved = memberships.save(m);
        return mapper.toDTO(saved);
    }

    @Override
    public void setRoleInTenant(UUID userId, String roleName) {
        UUID orgId = requireTenant();
        OrgMembership m = memberships.findByOrganization_OrgIdAndUser_UserId(orgId, userId)
                .orElseThrow(() -> new NoSuchElementException("Membership not found in current tenant"));
        m.setRoleName(roleName);
    }

    @Override
    public void setEnabledInTenant(UUID userId, boolean enabled) {
        UUID orgId = requireTenant();
        OrgMembership m = memberships.findByOrganization_OrgIdAndUser_UserId(orgId, userId)
                .orElseThrow(() -> new NoSuchElementException("Membership not found in current tenant"));
        m.setEnabled(enabled);
    }

    @Override
    public void removeMemberInTenant(UUID userId) {
        UUID orgId = requireTenant();
        OrgMembership m = memberships.findByOrganization_OrgIdAndUser_UserId(orgId, userId)
                .orElseThrow(() -> new NoSuchElementException("Membership not found in current tenant"));
        memberships.delete(m);
    }

    /* ---------------- helpers ---------------- */

    private UUID requireTenant() {
        TenantContext ctx = TenantContext.get();
        UUID orgId = (ctx != null && ctx.orgId().isPresent()) ? ctx.orgId().get() : null;
        if (orgId == null) throw new IllegalStateException("X-Org-Id is required");
        return orgId;
    }


}
