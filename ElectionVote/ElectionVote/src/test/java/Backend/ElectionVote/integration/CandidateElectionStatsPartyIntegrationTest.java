package Backend.ElectionVote.integration;


import Backend.ElectionVote.views.dto.CandidateElectionStatsPartyDto;
import Backend.ElectionVote.views.service.CandidateElectionStatsPartyService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for v_candidate_election_stats_party: builds minimal schema,
 * inserts vote_tally and v_election_stats_party and asserts aggregated results.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CandidateElectionStatsPartyIntegrationTest {

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
    private CandidateElectionStatsPartyService service;

    @Autowired
    private ElectionValidationService electionValidationService;

    @Autowired
    private StatsCacheEvictService cacheEvictService;

    private UUID orgId;
    private UUID electionId;
    private UUID candidateId;
    private UUID partyId;

    @BeforeAll
    void setupSchemaAndData() {
        // minimal schema
        jdbc.execute("CREATE TABLE IF NOT EXISTS candidate (candidate_id uuid PRIMARY KEY, full_name text, party_id uuid);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS party (party_id uuid PRIMARY KEY, party_name text, abbreviation text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS vote_tally (tally_id serial PRIMARY KEY, org_id uuid, election_id uuid, candidate_id uuid, vote_count int);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS election (election_id uuid PRIMARY KEY);");

        // helper fn_pct
        jdbc.execute("CREATE OR REPLACE FUNCTION fn_pct(numeric, numeric) RETURNS numeric LANGUAGE SQL AS $$ SELECT CASE WHEN $2 IS NULL OR $2 = 0 THEN NULL ELSE $1 / $2 END; $$;");

        // create v_election_stats_party minimal view (one row per org+election)
        jdbc.execute("CREATE OR REPLACE VIEW v_election_stats_party AS " +
                "SELECT org_id, election_id, SUM(registered_voters) AS registered_voters, SUM(ballots_cast) AS ballots_cast, SUM(valid_votes) AS valid_votes, SUM(invalid_total) AS invalid_total " +
                "FROM (VALUES (1,1,1,1,1)) AS t(r1,r2,r3,r4,r5) WHERE 1=0 GROUP BY org_id, election_id; -- placeholder; we'll create it concretely below");

        // We'll create a concrete v_election_stats_party by using a temp table approach:
        jdbc.execute("CREATE OR REPLACE VIEW v_election_stats_party AS " +
                "SELECT org_id, election_id, registered_voters, ballots_cast, valid_votes, invalid_total FROM (VALUES (1,1,0,0,0,0)) AS x(org_id,election_id,registered_voters,ballots_cast,valid_votes,invalid_total) WHERE false;");

        // For the test we'll create a materialized helper table and view replacement for a specific org/election
        orgId = UUID.randomUUID();
        electionId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        partyId = UUID.randomUUID();

        // Insert candidate/party and vote_tally row
        jdbc.update("INSERT INTO party(party_id, party_name, abbreviation) VALUES (?, ?, ?)", partyId, "Example Party", "EXP");
        jdbc.update("INSERT INTO candidate(candidate_id, full_name, party_id) VALUES (?, ?, ?)", candidateId, "Jane Doe", partyId);
        // insert election row so ElectionValidationService can find it (if it checks election table)
        jdbc.update("INSERT INTO election(election_id) VALUES (?)", electionId);

        // Insert vote_tally: candidate receives 1000 votes
        jdbc.update("INSERT INTO vote_tally(org_id, election_id, candidate_id, vote_count) VALUES (?, ?, ?, ?)", orgId, electionId, candidateId, 1000);

        // Create a concrete v_election_stats_party view that returns totals for our org+election
        // registered_voters 10000, ballots_cast 8000, valid_votes 7800, invalid_total 200
        jdbc.execute(String.format("CREATE OR REPLACE VIEW v_election_stats_party AS SELECT '%s'::uuid AS org_id, '%s'::uuid AS election_id, 10000 AS registered_voters, 8000 AS ballots_cast, 7800 AS valid_votes, 200 AS invalid_total", orgId, electionId));

        // sanity select from view
        var rows = jdbc.queryForList("SELECT * FROM v_election_stats_party");
        assertThat(rows).isNotEmpty();
    }

    @AfterAll
    void tearDown() { /* optional cleanup */ }

    @Test
    @Transactional
    void candidateElectionAggregation_returnsExpectedValues_and_cacheEviction() {
        // wiring
        assertThat(tenantGucService).isNotNull();
        assertThat(service).isNotNull();
        assertThat(electionValidationService).isNotNull();
        assertThat(cacheEvictService).isNotNull();

        // apply tenant GUC inside transaction
        tenantGucService.applyForTransaction(orgId, false, false);

        var page = service.listCandidateElectionStats(orgId, electionId, null, null, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);

        CandidateElectionStatsPartyDto dto = page.getContent().get(0);
        assertThat(dto.getCandidateVotes()).isEqualTo(1000);
        assertThat(dto.getRegisteredVoters()).isEqualTo(10000);
        assertThat(dto.getBallotsCast()).isEqualTo(8000);
        assertThat(dto.getValidVotes()).isEqualTo(7800);
        assertThat(dto.getInvalidTotal()).isEqualTo(200);

        // vote_share_pct = 1000 / 7800
        BigDecimal expectedShare = BigDecimal.valueOf(1000d).divide(BigDecimal.valueOf(7800d), 6, BigDecimal.ROUND_HALF_UP);
        assertThat(dto.getVoteSharePct()).isEqualByComparingTo(expectedShare);

        // evict targeted caches for this org+election
        cacheEvictService.evictOrgElectionCaches(orgId, electionId);
    }
}