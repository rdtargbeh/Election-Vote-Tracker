package Backend.ElectionVote.integration;


import Backend.ElectionVote.views.dto.ElectionStatsOfficialDto;
import Backend.ElectionVote.views.service.ElectionStatsOfficialService;
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
 * Integration test for v_election_stats_official.
 * Creates minimal v_center_stats_official and then v_election_stats_official, seeds data and asserts aggregated sums.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElectionStatsOfficialIntegrationTest {

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
    private ElectionStatsOfficialService service;

    @Autowired
    private TenantGucService tenantGucService;

    @Autowired
    private StatsCacheEvictService cacheEvictService;

    private UUID electionId;
    private UUID center1;
    private UUID center2;

    @BeforeAll
    void setupSchemaAndData() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS polling_center (center_id uuid PRIMARY KEY, district_id uuid, code text, center_name text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS district (district_id uuid PRIMARY KEY, district_name text, county_id uuid);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS county (county_id uuid PRIMARY KEY, county_name text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS polling_center_allocation (allocation_id serial PRIMARY KEY, org_id uuid, election_id uuid, center_id uuid, registered_voters int, ballots_cast int, valid_votes int, invalid_total int);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS nec_result (result_id serial PRIMARY KEY, org_id uuid, election_id uuid, center_id uuid, ballots_cast int, candidate_votes jsonb, invalid_ballots int, blank_ballots int, rejected_ballots int, spoiled_ballots int, is_published boolean);");

        jdbc.execute("CREATE OR REPLACE FUNCTION fn_sum_candidate_votes(jsonb) RETURNS int LANGUAGE SQL AS $$ SELECT (SELECT SUM((v::int)) FROM jsonb_each_text($1) as t(k,v)); $$;");
        jdbc.execute("CREATE OR REPLACE FUNCTION fn_pct(numeric, numeric) RETURNS numeric LANGUAGE SQL AS $$ SELECT CASE WHEN $2 IS NULL OR $2 = 0 THEN NULL ELSE $1 / $2 END; $$;");

        // create minimal v_center_stats_official view
        jdbc.execute("CREATE OR REPLACE VIEW v_center_stats_official AS " +
                "SELECT nr.election_id, nr.center_id, pc.code AS center_code, pc.center_name AS center_name, d.district_id, d.district_name, c.county_id, c.county_name, pca.registered_voters, " +
                "SUM(nr.ballots_cast) AS ballots_cast, SUM(fn_sum_candidate_votes(nr.candidate_votes))::int AS valid_votes, " +
                "SUM(nr.invalid_ballots + nr.blank_ballots + nr.rejected_ballots + nr.spoiled_ballots) AS invalid_total, " +
                "fn_pct(SUM(nr.ballots_cast)::numeric, pca.registered_voters::numeric) AS turnout_pct, " +
                "fn_pct(SUM(nr.invalid_ballots + nr.blank_ballots + nr.rejected_ballots + nr.spoiled_ballots)::numeric, SUM(nr.ballots_cast)::numeric) AS invalid_pct " +
                "FROM nec_result nr JOIN polling_center pc ON pc.center_id = nr.center_id JOIN district d ON d.district_id = pc.district_id JOIN county c ON c.county_id = d.county_id JOIN polling_center_allocation pca ON pca.center_id = nr.center_id AND pca.election_id = nr.election_id " +
                "WHERE nr.is_published = TRUE " +
                "GROUP BY nr.election_id, nr.center_id, pc.code, pc.center_name, d.district_id, d.district_name, c.county_id, c.county_name, pca.registered_voters;");

        // create election-level aggregation view
        jdbc.execute("CREATE OR REPLACE VIEW v_election_stats_official AS " +
                "SELECT election_id, SUM(registered_voters) AS registered_voters, SUM(ballots_cast) AS ballots_cast, SUM(valid_votes) AS valid_votes, SUM(invalid_total) AS invalid_total, " +
                "fn_pct(SUM(ballots_cast)::numeric, SUM(registered_voters)::numeric) AS turnout_pct, fn_pct(SUM(invalid_total)::numeric, SUM(ballots_cast)::numeric) AS invalid_pct " +
                "FROM v_center_stats_official GROUP BY election_id;");

        // seed data: two centers
        electionId = UUID.randomUUID();
        center1 = UUID.randomUUID();
        center2 = UUID.randomUUID();
        UUID district = UUID.randomUUID();
        UUID county = UUID.randomUUID();

        jdbc.update("INSERT INTO county(county_id, county_name) VALUES (?, ?)", county, "County E");
        jdbc.update("INSERT INTO district(district_id, district_name, county_id) VALUES (?, ?, ?)", district, "D1", county);
        jdbc.update("INSERT INTO polling_center(center_id, district_id, code, center_name) VALUES (?, ?, ?, ?)", center1, district, "C1", "Center 1");
        jdbc.update("INSERT INTO polling_center(center_id, district_id, code, center_name) VALUES (?, ?, ?, ?)", center2, district, "C2", "Center 2");

        // allocations
        jdbc.update("INSERT INTO polling_center_allocation(org_id, election_id, center_id, registered_voters, ballots_cast, valid_votes, invalid_total) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), electionId, center1, 1000, 600, 580, 20);
        jdbc.update("INSERT INTO polling_center_allocation(org_id, election_id, center_id, registered_voters, ballots_cast, valid_votes, invalid_total) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), electionId, center2, 800, 500, 480, 20);

        // nec_result rows (published)
        String candJson1 = "{\"" + UUID.randomUUID() + "\":\"350\"}";
        String candJson2 = "{\"" + UUID.randomUUID() + "\":\"430\"}";
        jdbc.update("INSERT INTO nec_result(org_id, election_id, center_id, ballots_cast, candidate_votes, invalid_ballots, blank_ballots, rejected_ballots, spoiled_ballots, is_published) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), electionId, center1, 600, candJson1, 10, 5, 3, 2, true);
        jdbc.update("INSERT INTO nec_result(org_id, election_id, center_id, ballots_cast, candidate_votes, invalid_ballots, blank_ballots, rejected_ballots, spoiled_ballots, is_published) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), electionId, center2, 500, candJson2, 10, 5, 3, 2, true);

        var rows = jdbc.queryForList("SELECT * FROM v_election_stats_official");
        assertThat(rows).isNotEmpty();
    }

    @Test
    @Transactional
    void electionAggregation_returnsExpectedValues() {
        // anonymous context
        tenantGucService.applyForTransaction((java.util.UUID) null, false, false);

        var page = service.listOfficialElections(electionId, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);

        ElectionStatsOfficialDto dto = page.getContent().get(0);
        // registered = 1000 + 800 = 1800
        assertThat(dto.getRegisteredVoters()).isEqualTo(1800);
        // ballots_cast = 600 + 500 = 1100
        assertThat(dto.getBallotsCast()).isEqualTo(1100);
        // valid_votes = 580 + 480 = 1060
        assertThat(dto.getValidVotes()).isEqualTo(1060);
        // invalid_total = 20 + 20 = 40
        assertThat(dto.getInvalidTotal()).isEqualTo(40);

        BigDecimal expectedTurnout = BigDecimal.valueOf(1100d).divide(BigDecimal.valueOf(1800d), 6, BigDecimal.ROUND_HALF_UP);
        assertThat(dto.getTurnoutPct()).isEqualByComparingTo(expectedTurnout);

        // Evict caches
        cacheEvictService.evictAllStatsCaches();
    }
}