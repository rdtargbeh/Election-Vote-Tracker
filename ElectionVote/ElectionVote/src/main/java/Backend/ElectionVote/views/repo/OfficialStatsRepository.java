//package Backend.ElectionVote.views.repo;
//
//import Backend.ElectionVote.views.OfficialCandidateCountyStatsView;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.Repository;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//import java.util.UUID;
//
//public interface OfficialStatsRepository extends Repository<Object, UUID> {
//
//    // All candidate stats by county for 1 election
//    @Query(
//            value = """
//            SELECT
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
//            FROM v_candidate_county_stats_official
//            WHERE election_id = :electionId
//            """,
//            nativeQuery = true
//    )
//    List<OfficialCandidateCountyStatsView> findCandidateCountyStatsByElection(
//            @Param("electionId") UUID electionId
//    );
//
//
//
//    // Optionally filtered by county
//    @Query(
//            value = """
//            SELECT
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
//            FROM v_candidate_county_stats_official
//            WHERE election_id = :electionId
//              AND county_id   = :countyId
//            """,
//            nativeQuery = true
//    )
//    List<OfficialCandidateCountyStatsView> findCandidateCountyStatsByElectionAndCounty(
//            @Param("electionId") UUID electionId,
//            @Param("countyId") UUID countyId
//    );
//}