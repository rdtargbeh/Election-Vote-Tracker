package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.AnomalyEvent;
import Backend.ElectionVote.enums.AnomalyKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;


@Repository
public interface AnomalyEventRepository extends JpaRepository<AnomalyEvent, UUID>, JpaSpecificationExecutor<AnomalyEvent> {

    @Query("select (count(a) > 0) from AnomalyEvent a " +
            "where a.organization.orgId = :orgId and a.election.electionId = :electionId " +
            "and a.pollingCenter.centerId = :centerId and a.kind = :kind and a.dateCreated >= :since")
    boolean existsRecentOfSameKindAtCenter(@Param("orgId") UUID orgId,
                                           @Param("electionId") UUID electionId,
                                           @Param("centerId") UUID centerId,
                                           @Param("kind") AnomalyKind kind,
                                           @Param("since") LocalDateTime since);
}

