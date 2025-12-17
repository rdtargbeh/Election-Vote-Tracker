package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.OrgMembership;
import Backend.ElectionVote.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrgMembershipRepository extends JpaRepository<OrgMembership, UUID> {

    /** Check membership existence for tenant-guarded operations. */
    boolean existsByOrganization_OrgIdAndUser_UserId(UUID orgId, UUID userId);

    /** Load a specific membership (e.g., to change org-scoped roleName). */
    Optional<OrgMembership> findByOrganization_OrgIdAndUser_UserId(UUID orgId, UUID userId);

    Optional<OrgMembership> findByOrganization_OrgIdAndUser_UserIdAndIsEnabledTrue(UUID orgId, UUID userId);


    @Query("""
       select m
       from OrgMembership m
         join m.user u
       where m.organization.orgId = :org
         and (:pattern is null or
              lower(u.userName)  like :pattern or
              lower(u.firstName) like :pattern or
              lower(u.lastName)  like :pattern or
              lower(u.email)     like :pattern)
         and (:role is null or m.roleName = :role)
         and (:enabled is null or m.isEnabled = :enabled)
       """)
    Page<OrgMembership> searchInOrg(@Param("org") UUID orgId,
                                    @Param("pattern") String pattern,
                                    @Param("role") String roleName,
                                    @Param("enabled") Boolean enabled,
                                    Pageable pageable);



    // Disable Org Member
    @Modifying
    @Query("""
       UPDATE OrgMembership m
       SET m.isEnabled = false
       WHERE m.organization.orgId = :orgId
       """)
    void disableAllForOrg(@Param("orgId") UUID orgId);

    @Query("select count(m) from OrgMembership m where m.organization.orgId = :orgId and upper(m.roleName) = :roleName and m.isEnabled = true")
    long countByOrganization_OrgIdAndRoleNameAndIsEnabledTrue(UUID orgId, String roleName);

    Page<OrgMembership> findByOrganization_OrgId(UUID orgId, Pageable pageable);

    Page<OrgMembership> findByUser_UserId(UUID userId, Pageable pageable);

    boolean existsByOrganization_OrgIdAndUser_UserIdAndIsEnabledTrue(UUID orgId, UUID userId);

    void deleteByOrganization_OrgIdAndUser_UserId(UUID orgId, UUID userId);
}
