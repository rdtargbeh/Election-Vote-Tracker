package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.UserSessionCreateRequest;
import Backend.ElectionVote.dto.UserSessionDto;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.SystemUser;
import Backend.ElectionVote.entity.UserSession;
import org.springframework.stereotype.Component;

@Component
public class UserSessionMapper {

    public UserSession toEntity(UserSessionCreateRequest req, SystemUser user, Organization org) {
        UserSession s = new UserSession();
        s.setUser(user);
        s.setOrganization(org);
        s.setExpiresDate(req.getExpiresDate());
        return s;
    }

    public UserSessionDto toDTO(UserSession s) {
        return UserSessionDto.builder()
                .sessionId(s.getSessionId())
                .userId(s.getUser().getUserId())
                .orgId(s.getOrganization() != null ? s.getOrganization().getOrgId() : null)
                .dateCreated(s.getDateCreated())
                .expiresDate(s.getExpiresDate())
                .revoked(s.isRevoked())
                .build();
    }
}
