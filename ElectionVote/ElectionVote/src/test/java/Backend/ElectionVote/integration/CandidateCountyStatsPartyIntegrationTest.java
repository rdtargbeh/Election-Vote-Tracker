package Backend.ElectionVote.integration;


import Backend.ElectionVote.views.dto.CandidateCountyStatsPartyDto;
import Backend.ElectionVote.views.service.CandidateCountyStatsPartyService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that sets up minimal schema, creates v_candidate_center_stats_party
 * then creates v_candidate_county_stats_party and asserts aggregated values.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CandidateCountyStatsPartyIntegrationTest {

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
        registry.add("spring.flyway.enabled", () -> false);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TenantGucService tenantGucService;

    @Autowired
    private CandidateCountyStatsPartyService service;

    @Autowired
    private ElectionValidationService electionValidationService;

    @Autowired
    private StatsCacheEvictService cacheEvictService;

    private UUID orgId;
    private UUID electionId;
    private UUID countyId;
    private UUID center1;
    private UUID center2;
    private UUID candidateId;

    @BeforeAll
    void setupSchemaAndData() {
        // minimal schema and helper function
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto;");
        jdbc.execute("CREATE TABLE IF NOT EXISTS county (county_id uuid PRIMARY KEY, county_name text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS district (district_id uuid PRIMARY KEY, district_name text, county_id uuid);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS polling_center (center_id uuid PRIMARY KEY, district_id uuid, code text, center_name text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS polling_center_allocation (allocation_id serial PRIMARY KEY, org_id uuid, election_id uuid, center_id uuid, registered_voters int, ballots_cast int, valid_votes int, invalid_total int);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS candidate (candidate_id uuid PRIMARY KEY, full_name text, party_id uuid);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS party (party_id uuid PRIMARY KEY, party_name text, abbreviation text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS vote_submission (vs_id serial PRIMARY KEY, org_id uuid, election_id uuid, center_id uuid, status text, date_deleted timestamp, candidate_votes jsonb);");
        jdbc.execute("CREATE OR REPLACE FUNCTION fn_pct(numeric, numeric) RETURNS numeric LANGUAGE SQL AS $$ SELECT CASE WHEN $2 IS NULL OR $2 = 0 THEN NULL ELSE $1 / $2 END; $$;");

        jdbc.execute("CREATE OR REPLACE VIEW v_center_stats_party AS " +
                "SELECT pca.org_id, pca.election_id, pca.center_id, pca.registered_voters, pca.ballots_cast, pca.valid_votes, pca.invalid_total, " +
                "fn_pct(pca.ballots_cast::numeric, pca.registered_voters::numeric) AS turnout_pct, fn_pct(pca.invalid_total::numeric, pca.ballots_cast::numeric) AS invalid_pct, pc.code AS center_code, pc.center_name " +
                "FROM polling_center_allocation pca JOIN polling_center pc ON pc.center_id = pca.center_id;");

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

        // create county-level view
        jdbc.execute("CREATE OR REPLACE VIEW v_candidate_county_stats_party AS " +
                "SELECT org_id, election_id, county_id, county_name, candidate_id, candidate_name, party_id, party_name, abbreviation, " +
                "SUM(candidate_votes) AS candidate_votes, SUM(ballots_cast) AS ballots_cast, SUM(center_valid_votes) AS total_valid_votes, SUM(center_invalid_total) AS total_invalid_votes, " +
                "fn_pct(SUM(candidate_votes)::numeric, NULLIF(SUM(center_valid_votes),0)::numeric) AS vote_share_pct " +
                "FROM v_candidate_center_stats_party GROUP BY org_id, election_id, county_id, county_name, candidate_id, candidate_name, party_id, party_name, abbreviation;");

        // seed: two centers in same county
        orgId = UUID.randomUUID();
        electionId = UUID.randomUUID();
        countyId = UUID.randomUUID();
        center1 = UUID.randomUUID();
        center2 = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        jdbc.update("INSERT INTO county(county_id, county_name) VALUES (?, ?)", countyId, "County X");
        UUID district1 = UUID.randomUUID();
        UUID district2 = UUID.randomUUID();
        jdbc.update("INSERT INTO district(district_id, district_name, county_id) VALUES (?, ?, ?)", district1, "D1", countyId);
        jdbc.update("INSERT INTO district(district_id, district_name, county_id) VALUES (?, ?, ?)", district2, "D2", countyId);
        jdbc.update("INSERT INTO polling_center(center_id, district_id, code, center_name) VALUES (?, ?, ?, ?)", center1, district1, "CTR-001", "Center 1");
        jdbc.update("INSERT INTO polling_center(center_id, district_id, code, center_name) VALUES (?, ?, ?, ?)", center2, district2, "CTR-002", "Center 2");
        jdbc.update("INSERT INTO polling_center_allocation(org_id, election_id, center_id, registered_voters, ballots_cast, valid_votes, invalid_total) VALUES (?, ?, ?, ?, ?, ?, ?)",
                orgId, electionId, center1, 500, 300, 290, 10);
        jdbc.update("INSERT INTO polling_center_allocation(org_id, election_id, center_id, registered_voters, ballots_cast, valid_votes, invalid_total) VALUES (?, ?, ?, ?, ?, ?, ?)",
                orgId, electionId, center2, 700, 400, 380, 20);
        jdbc.update("INSERT INTO candidate(candidate_id, full_name, party_id) VALUES (?, ?, ?)", candidateId, "Jane Doe", partyId);
        jdbc.update("INSERT INTO party(party_id, party_name, abbreviation) VALUES (?, ?, ?)", partyId, "Example Party", "EXP");

        // candidate votes: center1 = 150, center2 = 100 -> total 250
        String json1 = String.format("{\"%s\":\"150\"}", candidateId.toString());
        String json2 = String.format("{\"%s\":\"100\"}", candidateId.toString());
        jdbc.update("INSERT INTO vote_submission(org_id, election_id, center_id, status, date_deleted, candidate_votes) VALUES (?, ?, ?, ?, NULL, ?::jsonb)",
                orgId, electionId, center1, "VERIFIED", json1);
        jdbc.update("INSERT INTO vote_submission(org_id, election_id, center_id, status, date_deleted, candidate_votes) VALUES (?, ?, ?, ?, NULL, ?::jsonb)",
                orgId, electionId, center2, "VERIFIED", json2);

        List<CandidateCountyStatsPartyDto> rows = jdbc.query(
                "SELECT * FROM v_candidate_county_stats_party",
                (rs, rowNum) -> {
                    CandidateCountyStatsPartyDto d = new CandidateCountyStatsPartyDto();
                    d.setOrgId(UUID.fromString(rs.getString("org_id")));
                    d.setElectionId(UUID.fromString(rs.getString("election_id")));
                    d.setCountyId(UUID.fromString(rs.getString("county_id")));
                    d.setCandidateId(UUID.fromString(rs.getString("candidate_id")));
                    d.setCandidateVotes(rs.getInt("candidate_votes"));
                    d.setTotalValidVotes(rs.getInt("total_valid_votes"));
                    d.setVoteSharePct(rs.getBigDecimal("vote_share_pct"));
                    return d;
                });
        assertThat(rows).isNotEmpty();
    }

    @AfterAll
    void tearDown() { /* optional cleanup */ }

    @Test
    @Transactional
    void candidateCountyAggregation_returnsExpectedValues_and_cacheEviction() {
        assertThat(tenantGucService).isNotNull();
        assertThat(service).isNotNull();
        assertThat(electionValidationService).isNotNull();
        assertThat(cacheEvictService).isNotNull();

        tenantGucService.applyForTransaction(orgId, false, false);

        var page = service.listCandidateCountyStats(orgId, electionId, null, null, null, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);

        CandidateCountyStatsPartyDto dto = page.getContent().get(0);
        // candidate_votes = 150 + 100 = 250
        assertThat(dto.getCandidateVotes()).isEqualTo(250);
        // total_valid_votes = 290 + 380 = 670
        assertThat(dto.getTotalValidVotes()).isEqualTo(670);
        // vote_share_pct = 250 / 670
        BigDecimal expectedShare = BigDecimal.valueOf(250d).divide(BigDecimal.valueOf(670d), 6, BigDecimal.ROUND_HALF_UP);
        assertThat(dto.getVoteSharePct()).isEqualByComparingTo(expectedShare);

        // demonstrate targeted eviction
        cacheEvictService.evictOrgElectionCaches(orgId, electionId);
    }
}