package Backend.ElectionVote.integration;


import Backend.ElectionVote.views.dto.CandidateCenterStatsPartyDto;
import Backend.ElectionVote.views.repo.CandidateCenterStatsPartyRepository;
import Backend.ElectionVote.views.service.CandidateCenterStatsPartyService;
import Backend.ElectionVote.views.service.ElectionValidationService;
import Backend.ElectionVote.views.service.StatsCacheEvictService;
import Backend.ElectionVote.views.service.TenantGucService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that builds minimal schema/views and asserts aggregated results from
 * v_candidate_center_stats_party using JdbcTemplate for setup.
 *
 * This is a full end-to-end example for the candidate-center view and should be extended
 * for more cases (multiple candidates/centers/orgs/elections and RLS scenarios).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CandidateCenterStatsPartyIntegrationTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // enable flyway if you have test migrations
        registry.add("spring.flyway.enabled", () -> false);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TenantGucService tenantGucService;

    @Autowired
    private CandidateCenterStatsPartyService service;

    @Autowired
    private CandidateCenterStatsPartyRepository repo;

    @Autowired
    private ElectionValidationService electionValidationService;

    @Autowired
    private StatsCacheEvictService cacheEvictService;

    private UUID orgId;
    private UUID electionId;
    private UUID countyId;
    private UUID districtId;
    private UUID centerId;
    private UUID candidateId;
    private UUID partyId;

    @BeforeAll
    void setupSchemaAndData() {
        // Minimal schema (tables and helper function) required for the views to work
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto;");
        jdbc.execute("CREATE TABLE IF NOT EXISTS county (county_id uuid PRIMARY KEY, county_name text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS district (district_id uuid PRIMARY KEY, district_name text, county_id uuid);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS polling_center (center_id uuid PRIMARY KEY, district_id uuid, code text, center_name text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS polling_center_allocation (allocation_id serial PRIMARY KEY, org_id uuid, election_id uuid, center_id uuid, registered_voters int, ballots_cast int, valid_votes int, invalid_total int);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS candidate (candidate_id uuid PRIMARY KEY, full_name text, party_id uuid);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS party (party_id uuid PRIMARY KEY, party_name text, abbreviation text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS vote_submission (vs_id serial PRIMARY KEY, org_id uuid, election_id uuid, center_id uuid, status text, date_deleted timestamp, candidate_votes jsonb);");

        // helper function: safe percentage
        jdbc.execute("CREATE OR REPLACE FUNCTION fn_pct(numeric, numeric) RETURNS numeric LANGUAGE SQL AS $$ SELECT CASE WHEN $2 IS NULL OR $2 = 0 THEN NULL ELSE $1 / $2 END; $$;");

        // v_center_stats_party (minimal) - source for candidate view
        jdbc.execute("CREATE OR REPLACE VIEW v_center_stats_party AS " +
                "SELECT pca.org_id, pca.election_id, pca.center_id, pc.code AS center_code, pc.center_name, pca.registered_voters, pca.ballots_cast, pca.valid_votes, pca.invalid_total, " +
                "fn_pct(pca.ballots_cast::numeric, pca.registered_voters::numeric) AS turnout_pct, fn_pct(pca.invalid_total::numeric, pca.ballots_cast::numeric) AS invalid_pct " +
                "FROM polling_center_allocation pca JOIN polling_center pc ON pc.center_id = pca.center_id;");

        // v_candidate_center_stats_party as provided
        jdbc.execute("CREATE OR REPLACE VIEW v_candidate_center_stats_party AS " +
                "WITH exploded AS ( " +
                "  SELECT vs.org_id, vs.election_id, vs.center_id, (j.key::uuid) AS candidate_id, (j.value::int) AS candidate_votes " +
                "  FROM vote_submission vs CROSS JOIN LATERAL jsonb_each_text(vs.candidate_votes) AS j(key,value) " +
                "  WHERE vs.status = 'VERIFIED' AND vs.date_deleted IS NULL " +
                ") " +
                "SELECT e.org_id, e.election_id, c.county_id, c.county_name, d.district_id, d.district_name, pc.center_id, pc.code AS center_code, pc.center_name AS center_name, " +
                "e.candidate_id, cand.full_name AS candidate_name, cand.party_id, p.party_name, p.abbreviation AS party_code, SUM(e.candidate_votes) AS candidate_votes, " +
                "cs.registered_voters, cs.ballots_cast, cs.valid_votes AS center_valid_votes, cs.invalid_total AS center_invalid_total, " +
                "fn_pct(SUM(e.candidate_votes)::numeric, NULLIF(cs.valid_votes,0)::numeric) AS vote_share_pct " +
                "FROM exploded e " +
                "JOIN polling_center pc ON pc.center_id = e.center_id " +
                "JOIN district d ON d.district_id = pc.district_id " +
                "JOIN county c ON c.county_id = d.county_id " +
                "JOIN candidate cand ON cand.candidate_id = e.candidate_id " +
                "LEFT JOIN party p ON p.party_id = cand.party_id " +
                "JOIN v_center_stats_party cs ON cs.org_id = e.org_id AND cs.election_id = e.election_id AND cs.center_id = e.center_id " +
                "GROUP BY e.org_id, e.election_id, c.county_id, c.county_name, d.district_id, d.district_name, pc.center_id, pc.code, pc.center_name, e.candidate_id, cand.full_name, cand.party_id, p.party_name, p.abbreviation, cs.registered_voters, cs.ballots_cast, cs.valid_votes, cs.invalid_total;");

        // Insert sample data
        orgId = UUID.randomUUID();
        electionId = UUID.randomUUID();
        countyId = UUID.randomUUID();
        districtId = UUID.randomUUID();
        centerId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        partyId = UUID.randomUUID();

        jdbc.update("INSERT INTO county(county_id, county_name) VALUES (?, ?)", countyId, "Example County");
        jdbc.update("INSERT INTO district(district_id, district_name, county_id) VALUES (?, ?, ?)", districtId, "North District", countyId);
        jdbc.update("INSERT INTO polling_center(center_id, district_id, code, center_name) VALUES (?, ?, ?, ?)", centerId, districtId, "CTR-001", "Central High School");
        // allocation: registered 1200, ballots cast 850, valid 830, invalid 20
        jdbc.update("INSERT INTO polling_center_allocation(org_id, election_id, center_id, registered_voters, ballots_cast, valid_votes, invalid_total) VALUES (?, ?, ?, ?, ?, ?, ?)",
                orgId, electionId, centerId, 1200, 850, 830, 20);
        jdbc.update("INSERT INTO candidate(candidate_id, full_name, party_id) VALUES (?, ?, ?)", candidateId, "Jane Doe", partyId);
        jdbc.update("INSERT INTO party(party_id, party_name, abbreviation) VALUES (?, ?, ?)", partyId, "Example Party", "EXP");

        // candidate_votes JSONB: candidate -> votes
        String candidateJson = String.format("{\"%s\":\"100\"}", candidateId.toString());
        jdbc.update("INSERT INTO vote_submission(org_id, election_id, center_id, status, date_deleted, candidate_votes) VALUES (?, ?, ?, ?, NULL, ?::jsonb)",
                orgId, electionId, centerId, "VERIFIED", candidateJson);

        // sanity: ensure view materializes correctly by selecting it
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM v_candidate_center_stats_party");
        assertThat(rows).isNotEmpty();
    }

    @AfterAll
    void tearDown() {
        // cleanup if desired
    }

    @Test
    @Transactional
    void candidateCenterAggregation_returnsExpectedValues_and_canApplyTenantGuc() {
        // confirm bean wiring
        assertThat(tenantGucService).isNotNull();
        assertThat(service).isNotNull();
        assertThat(repo).isNotNull();
        assertThat(electionValidationService).isNotNull();

        // apply GUC in transaction
        tenantGucService.applyForTransaction(orgId, false, false);

        // call service to fetch candidate center stats
        var page = service.listCandidateCenterStats(orgId, electionId, null, null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);

        CandidateCenterStatsPartyDto dto = page.getContent().get(0);
        assertThat(dto.getCandidateVotes()).isEqualTo(100);
        assertThat(dto.getRegisteredVoters()).isEqualTo(1200);
        assertThat(dto.getBallotsCast()).isEqualTo(850);
        assertThat(dto.getCenterValidVotes()).isEqualTo(830);
        // vote_share_pct = 100 / 830
        BigDecimal expectedShare = BigDecimal.valueOf(100d).divide(BigDecimal.valueOf(830d), 6, BigDecimal.ROUND_HALF_UP);
        assertThat(dto.getVoteSharePct()).isEqualByComparingTo(expectedShare);

        // Demonstrate cache eviction API (to be called from write-paths)
        cacheEvictService.evictAllStatsCaches();
    }
}