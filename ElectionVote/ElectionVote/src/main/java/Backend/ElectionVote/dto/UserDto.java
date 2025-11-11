package Backend.ElectionVote.dto;

import jakarta.persistence.Column;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private UUID userId;
    private String firstName;
    private String lastName;
    private String userName;
    private String email;
    private String phoneNumber;
    private boolean isActive;
    private boolean isVerified;
    private String roleName;           // e.g., "ADMIN", "AGENT"
    private UUID partyId;              // nullable
    private UUID assignedCountyId;     // nullable
    private UUID defaultOrgId;         // nullable
    private LocalDateTime lastLogin;   // nullable
    private LocalDateTime dateCreated;
    private int failedLoginAttempts = 0;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastPasswordChange;

}
