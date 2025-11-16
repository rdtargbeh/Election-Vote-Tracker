package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.OrgMembership;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.Party;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartyRepository extends JpaRepository<Party, UUID> {

    boolean existsByPartyNameIgnoreCase(String partyName);
    boolean existsByAbbreviationIgnoreCase(String abbreviation);

    boolean existsByPartyNameIgnoreCaseAndPartyIdNot(String partyName, UUID excludeId);
    boolean existsByAbbreviationIgnoreCaseAndPartyIdNot(String abbreviation, UUID excludeId);

    Optional<Party> findByAbbreviationIgnoreCase(String abbreviation);

    @Query("""
           select p from Party p
           where (:q is null or
                  lower(p.partyName) like lower(concat('%', :q, '%')) or
                  lower(p.abbreviation) like lower(concat('%', :q, '%')))
           """)
    Page<Party> search(@Param("q") String q, Pageable pageable);


}
