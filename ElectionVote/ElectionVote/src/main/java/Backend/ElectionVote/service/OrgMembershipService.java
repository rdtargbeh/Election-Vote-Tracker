package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.MemberSearchRequest;
import Backend.ElectionVote.dto.MembershipCreateRequest;
import Backend.ElectionVote.dto.OrgMembershipDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrgMembershipService {

    Page<OrgMembershipDto> listInTenant(MemberSearchRequest req, Pageable pageable);

    OrgMembershipDto addMemberInTenant(MembershipCreateRequest req);

    void setRoleInTenant(UUID userId, String roleName);

    void setEnabledInTenant(UUID userId, boolean enabled);

    void removeMemberInTenant(UUID userId); // delete membership row only
}
