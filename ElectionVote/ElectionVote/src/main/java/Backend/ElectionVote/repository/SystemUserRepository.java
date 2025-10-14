package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.SystemUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SystemUserRepository extends JpaRepository<SystemUser, UUID>, JpaSpecificationExecutor<SystemUser> {

    Optional<SystemUser> findByEmailIgnoreCase(String email);
    Optional<SystemUser> findByUserNameIgnoreCase(String userName);

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUserNameIgnoreCase(String userName);

    @Query("""
           select u
           from SystemUser u
           where exists (
               select 1
               from OrgMembership m
               where m.organization.orgId = :org
                 and m.user.userId       = u.userId
                 and m.enabled           = true
           )
           and (:q is null or
                lower(u.firstName) like lower(concat('%', :q, '%')) or
                lower(u.lastName)  like lower(concat('%', :q, '%')) or
                lower(u.email)     like lower(concat('%', :q, '%')) or
                lower(u.userName)  like lower(concat('%', :q, '%')))
           and (:active is null or u.isActive = :active)
           """)
    Page<SystemUser> findAllInOrg(@Param("org") UUID orgId,
                                  @Param("q") String q,
                                  @Param("active") Boolean active,
                                  Pageable pageable);
}