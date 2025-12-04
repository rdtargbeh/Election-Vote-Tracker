package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.UserCreateRequest;
import Backend.ElectionVote.dto.UserDto;
import Backend.ElectionVote.dto.UserUpdateRequest;
import Backend.ElectionVote.entity.*;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDTO(SystemUser user){
        if (user == null) return null;

        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setActive(user.isActive());
        dto.setVerified(user.isVerified());


        // role enum -> String (change UserDto to enum if you prefer)
        dto.setRoleName(user.getRole() != null ? user.getRole().getRoleName().name() : null);
        dto.setPartyId(user.getParty() != null ? user.getParty().getPartyId() : null);
        dto.setAssignedCountyId(user.getAssignedCounty() != null ? user.getAssignedCounty().getCountyId() : null);
        dto.setDefaultOrgId(user.getDefaultOrg() != null ? user.getDefaultOrg().getOrgId() : null);

        dto.setLastLogin(user.getLastLogin());
        dto.setDateCreated(user.getDateCreated());
        dto.setDateUpdated(user.getDateUpdated());

        // expose signing key id if associated
        dto.setSigningKeyId(user.getSigningKeyId());

        dto.setFailedLoginAttempts(user.getFailedLoginAttempts());
        dto.setLockedUntil(user.getLockedUntil());
        dto.setLastPasswordChange(user.getLastPasswordChange());



        return dto;
    }

    /**
     * Create a new SystemUser from CreateUserRequest.
     * NOTE: Does NOT set relations (role/party/county/org) and does NOT set passwordHash.
     * The service should attach those after lookups and encode the password.
     */
    public SystemUser toEntity(UserCreateRequest req) {
        if (req == null) return null;

        SystemUser u = new SystemUser();
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setUserName(req.getUserName());
        u.setEmail(req.getEmail());
        u.setPhoneNumber(req.getPhoneNumber());

        return u;
    }

    /**
     * Fully-hydrating toEntity for "create" when the service has already
     * loaded references and encoded the password.
     */
    public SystemUser toEntity(UserCreateRequest req,
                               UserRole role,
                               Party party,
                               County county,
                               Organization defaultOrg,
                               String encodedPassword) {
        if (req == null) return null;
        SystemUser u = toEntity(req);
        u.setPassword(encodedPassword); // already encoded by service
        u.setRole(role);
        u.setParty(party);
        u.setAssignedCounty(county);
        u.setDefaultOrg(defaultOrg);
        return u;
    }

    /**
     * Apply scalar updates from UpdateUserRequest to an existing entity.
     * Relationships (role/party/county/org) should be set in the service after lookups.
     */
    public void applyUpdate(UserUpdateRequest req, SystemUser user) {
        if (req == null || user == null) return;

        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setUserName(req.getUserName());
        user.setEmail(req.getEmail());
        user.setPhoneNumber(req.getPhoneNumber());

        if (Boolean.TRUE.equals(req.getActive()) || Boolean.FALSE.equals(req.getActive())) {
            user.setActive(req.getActive());
        }
        if (Boolean.TRUE.equals(req.getVerified()) || Boolean.FALSE.equals(req.getVerified())) {
            user.setVerified(req.getVerified());
            if (Boolean.TRUE.equals(req.getVerified())) {
                user.setFailedLoginAttempts(0);
            }
        }

    }

}
