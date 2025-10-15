package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.OrgMembership;
import Backend.ElectionVote.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrgMembershipRepository extends JpaRepository<OrgMembership, UUID> {

    /** Check membership existence for tenant-guarded operations. */
    boolean existsByOrganization_OrgIdAndUser_UserId(UUID orgId, UUID userId);

    /** Load a specific membership (e.g., to change org-scoped roleName). */
    Optional<OrgMembership> findByOrganization_OrgIdAndUser_UserId(UUID orgId, UUID userId);

    Page<OrgMembership> findByOrganization_OrgId(UUID orgId, Pageable pageable);

    Page<OrgMembership> findByUser_UserId(UUID userId, Pageable pageable);


    boolean existsByOrganization_OrgIdAndUser_UserIdAndIsEnabledTrue(UUID orgId, UUID userId);

    Optional<OrgMembership> findByOrganization_OrgIdAndUser_UserIdAndIsEnabledTrue(UUID orgId, UUID userId);


    @Query("""
           select m from OrgMembership m
             join m.user u
           where m.organization.orgId = :org
             and (:q is null or
                 lower(u.userName)  like lower(concat('%', :q, '%')) or
                 lower(u.firstName) like lower(concat('%', :q, '%')) or
                 lower(u.lastName)  like lower(concat('%', :q, '%')) or
                 lower(u.email)     like lower(concat('%', :q, '%')))
             and (:role is null or m.roleName = :role)
             and (:enabled is null or m.isEnabled = :enabled)
           """)
    Page<OrgMembership> searchInOrg(@Param("org") UUID orgId,
                                    @Param("q") String q,
                                    @Param("role") String roleName,
                                    @Param("enabled") Boolean enabled,
                                    Pageable pageable);
}
