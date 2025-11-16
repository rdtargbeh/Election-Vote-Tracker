package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.enums.OrganizationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findBySubdomainIgnoreCase(String subdomain);

    boolean existsBySubdomainIgnoreCase(String subdomain);

    boolean existsBySubdomainIgnoreCaseAndOrgIdNot(String subdomain, UUID orgId);

//    boolean existsByParty_PartyId(UUID partyId);

    boolean existsByOrgIdAndIsActiveTrue(UUID orgId);


    @Query("select o.orgId from Organization o where lower(o.subdomain) = lower(?1) and o.isActive = true")
    Optional<UUID> findIdBySubdomainIgnoreCaseAndIsActiveTrue(String subdomain);


    @Query("""
           select o from Organization o
           where (:q is null or lower(o.orgName) like lower(concat('%', :q, '%'))
                          or lower(o.subdomain) like lower(concat('%', :q, '%')))
             and (:active is null or o.isActive = :active)
             and (:type is null or o.organizationType = :type)
           """)
    Page<Organization> search(@Param("q") String q,
                              @Param("active") Boolean active,
                              @Param("type") OrganizationType type,
                              Pageable pageable);



}
