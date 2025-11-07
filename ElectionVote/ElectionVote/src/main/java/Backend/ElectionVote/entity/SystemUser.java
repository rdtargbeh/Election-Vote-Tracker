package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(
        name = "system_users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_email", columnNames = "email")
        },
        indexes = {
                @Index(name="idx_users_email", columnList="email"),
                @Index(name="idx_users_username", columnList="user_name")
        }
)

public class SystemUser {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 30)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 30)
    private String lastName;

    @Column(name = "user_name", nullable = false, unique = true, length = 30)
    private String userName;

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    /** Relationships **/

    // Role reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_role"))
    private UserRole role;

    // Party reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", foreignKey = @ForeignKey(name = "fk_user_party"))
    private Party party;

    // County assignment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_county", foreignKey = @ForeignKey(name = "fk_user_county"))
    private County assignedCounty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_org_id", foreignKey = @ForeignKey(name = "fk_user_default_org"))
    private Organization defaultOrg;

    /** Status flags **/
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "is_system_admin", nullable = false)
    private boolean isSystemAdmin;  // maps to SQL boolean column

    /** Audit fields **/
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /** Security / audit **/
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_password_change")
    private LocalDateTime lastPasswordChange;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated = LocalDateTime.now();


    // GETTER & SETTER


    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public County getAssignedCounty() {
        return assignedCounty;
    }

    public void setAssignedCounty(County assignedCounty) {
        this.assignedCounty = assignedCounty;
    }

    public Organization getDefaultOrg() {
        return defaultOrg;
    }

    public void setDefaultOrg(Organization defaultOrg) {
        this.defaultOrg = defaultOrg;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public boolean isSystemAdmin() {
        return isSystemAdmin;
    }

    public void setSystemAdmin(boolean systemAdmin) {
        isSystemAdmin = systemAdmin;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getLastPasswordChange() {
        return lastPasswordChange;
    }

    public void setLastPasswordChange(LocalDateTime lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }
}
