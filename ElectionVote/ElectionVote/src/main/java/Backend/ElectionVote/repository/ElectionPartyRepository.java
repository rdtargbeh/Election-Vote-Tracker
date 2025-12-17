package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.ElectionParty;
import Backend.ElectionVote.utility.ElectionPartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ElectionPartyRepository extends JpaRepository<ElectionParty, ElectionPartyId> {

    boolean existsByElection_ElectionIdAndParty_PartyId(UUID electionId, UUID partyId);

    List<ElectionParty> findByElection_ElectionIdOrderByBallotOrderAscParty_PartyNameAsc(UUID electionId);

    @Query("select coalesce(max(ep.ballotOrder), 0) from ElectionParty ep where ep.election.electionId = :electionId")
    int findMaxBallotOrder(@Param("electionId") UUID electionId);


    List<ElectionParty> findByElection_ElectionIdOrderByBallotOrderAsc(UUID electionId);



}

