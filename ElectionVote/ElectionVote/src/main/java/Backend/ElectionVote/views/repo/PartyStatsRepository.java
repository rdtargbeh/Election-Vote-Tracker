//package Backend.ElectionVote.views.repo;
//
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.Repository;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//import java.util.UUID;
//
//public interface PartyStatsRepository extends Repository<Object, UUID> {
//
//
//    @Query(
//            value = """
//            SELECT
//              org_id,
//              election_id,
//              county_id,
//              county_name,
//              candidate_id,
//              candidate_name,
//              party_id,
//              party_name,
//              party_code,
//              candidate_votes,
//              ballots_cast,
//              total_valid_votes,
//              total_invalid_votes,
//              vote_share_pct
//            FROM v_candidate_county_stats_party
//            WHERE org_id      = :orgId
//              AND election_id = :electionId
//            """,
//            nativeQuery = true
//    )
//    List<PartyCandidateCountyStatsView> findCandidateCountyStatsByOrgAndElection(
//            @Param("orgId") UUID orgId,
//            @Param("electionId") UUID electionId
//    );
//}