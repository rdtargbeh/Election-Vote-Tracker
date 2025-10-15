package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.OrgMembershipDto;
import Backend.ElectionVote.entity.OrgMembership;
import org.springframework.stereotype.Component;

@Component
public class OrgMembershipMapper {

    public OrgMembershipDto toDTO(OrgMembership m) {
        if (m == null) return null;

        OrgMembershipDto dto = new OrgMembershipDto();
        dto.setMembershipId(m.getMembershipId());
        dto.setOrgId(m.getOrganization() != null ? m.getOrganization().getOrgId() : null);
        dto.setUserId(m.getUser() != null ? m.getUser().getUserId() : null);
        dto.setRoleName(m.getRoleName());
        dto.setEnabled(m.isEnabled());

        return dto;
    }
}