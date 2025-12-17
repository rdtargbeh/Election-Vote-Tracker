//package Backend.ElectionVote.views.repo;
//
//import Backend.ElectionVote.views.CandidateCountyCompareView;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.Repository;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//import java.util.UUID;
//
//public interface CandidateCompareRepository extends Repository<Object, UUID> {
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
//              party_candidate_votes,
//              official_candidate_votes,
//              diff_votes,
//              party_vote_share_pct,
//              official_vote_share_pct
//            FROM v_candidate_county_compare
//            WHERE org_id      = :orgId
//              AND election_id = :electionId
//            """,
//            nativeQuery = true
//    )
//    List<CandidateCountyCompareView> findCandidateCountyCompare(
//            @Param("orgId") UUID orgId,
//            @Param("electionId") UUID electionId
//    );
//}