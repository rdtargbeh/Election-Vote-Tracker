package Backend.ElectionVote.integration;


import Backend.ElectionVote.views.dto.CandidateElectionStatsOfficialDto;
import Backend.ElectionVote.views.service.CandidateElectionStatsOfficialService;
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
 * Integration test for v_candidate_election_stats_official.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CandidateElectionStatsOfficialIntegrationTest {

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
    private CandidateElectionStatsOfficialService service;

    @Autowired
    private TenantGucService tenantGucService;

    @Autowired
    private StatsCacheEvictService cacheEvictService;

    private UUID electionId;
    private UUID center1;
    private UUID center2;
    private UUID district;
    private UUID county;
    private UUID candidateId;
    private UUID partyId;

    @BeforeAll
    void setupSchemaAndData() {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto;");
        jdbc.execute("CREATE TABLE IF NOT EXISTS county (county_id uuid PRIMARY KEY, county_name text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS district (district_id uuid PRIMARY KEY, district_name text, county_id uuid);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS polling_center (center_id uuid PRIMARY KEY, district_id uuid, code text, center_name text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS polling_center_allocation (allocation_id serial PRIMARY KEY, org_id uuid, election_id uuid, center_id uuid, registered_voters int, ballots_cast int, valid_votes int, invalid_total int);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS candidate (candidate_id uuid PRIMARY KEY, full_name text, party_id uuid);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS party (party_id uuid PRIMARY KEY, party_name text, abbreviation text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS nec_result (result_id serial PRIMARY KEY, org_id uuid, election_id uuid, center_id uuid, ballots_cast int, candidate_votes jsonb, invalid_ballots int, blank_ballots int, rejected_ballots int, spoiled_ballots int, is_published boolean);");

        jdbc.execute("CREATE OR REPLACE FUNCTION fn_sum_candidate_votes(jsonb) RETURNS int LANGUAGE SQL AS $$ SELECT (SELECT SUM((v::int)) FROM jsonb_each_text($1) as t(k,v)); $$;");
        jdbc.execute("CREATE OR REPLACE FUNCTION fn_pct(numeric, numeric) RETURNS numeric LANGUAGE SQL AS $$ SELECT CASE WHEN $2 IS NULL OR $2 = 0 THEN NULL ELSE $1 / $2 END; $$;");

        // create v_center_stats_official and candidate center/election views
        jdbc.execute("CREATE OR REPLACE VIEW v_center_stats_official AS " +
                "SELECT nr.election_id, nr.center_id, pc.code AS center_code, pc.center_name AS center_name, d.district_id, d.district_name, c.county_id, c.county_name, pca.registered_voters, " +
                "SUM(nr.ballots_cast) AS ballots_cast, SUM(fn_sum_candidate_votes(nr.candidate_votes))::int AS valid_votes, " +
                "SUM(nr.invalid_ballots + nr.blank_ballots + nr.rejected_ballots + nr.spoiled_ballots) AS invalid_total, " +
                "fn_pct(SUM(nr.ballots_cast)::numeric, pca.registered_voters::numeric) AS turnout_pct, " +
                "fn_pct(SUM(nr.invalid_ballots + nr.blank_ballots + nr.rejected_ballots + nr.spoiled_ballots)::numeric, SUM(nr.ballots_cast)::numeric) AS invalid_pct " +
                "FROM nec_result nr JOIN polling_center pc ON pc.center_id = nr.center_id JOIN district d ON d.district_id = pc.district_id JOIN county c ON c.county_id = d.county_id JOIN polling_center_allocation pca ON pca.center_id = nr.center_id AND pca.election_id = nr.election_id " +
                "WHERE nr.is_published = TRUE " +
                "GROUP BY nr.election_id, nr.center_id, pc.code, pc.center_name, d.district_id, d.district_name, c.county_id, c.county_name, pca.registered_voters;");

        jdbc.execute("CREATE OR REPLACE VIEW v_candidate_center_stats_official AS " +
                "WITH exploded AS ( " +
                "  SELECT nr.election_id, nr.center_id, (j.key::uuid) AS candidate_id, (j.value::int) AS candidate_votes " +
                "  FROM nec_result nr CROSS JOIN LATERAL jsonb_each_text(nr.candidate_votes) AS j(key,value) " +
                "  WHERE nr.is_published = TRUE " +
                ") " +
                "SELECT e.election_id, c.county_id, c.county_name, d.district_id, d.district_name, pc.center_id, pc.code AS center_code, pc.center_name AS center_name, e.candidate_id, cand.full_name AS candidate_name, cand.party_id, p.party_name, p.abbreviation AS party_code, SUM(e.candidate_votes) AS candidate_votes, cs.registered_voters, cs.ballots_cast, cs.valid_votes AS center_valid_votes, cs.invalid_total AS center_invalid_total, fn_pct(SUM(e.candidate_votes)::numeric, NULLIF(cs.valid_votes,0)::numeric) AS vote_share_pct " +
                "FROM exploded e JOIN polling_center pc ON pc.center_id = e.center_id JOIN district d ON d.district_id = pc.district_id JOIN county c ON c.county_id = d.county_id JOIN candidate cand ON cand.candidate_id = e.candidate_id LEFT JOIN party p ON p.party_id = cand.party_id JOIN v_center_stats_official cs ON cs.election_id = e.election_id AND cs.center_id = e.center_id " +
                "GROUP BY e.election_id, c.county_id, c.county_name, d.district_id, d.district_name, pc.center_id, pc.code, pc.center_name, e.candidate_id, cand.full_name, cand.party_id, p.party_name, p.abbreviation, cs.registered_voters, cs.ballots_cast, cs.valid_votes, cs.invalid_total;");

        jdbc.execute("CREATE OR REPLACE VIEW v_candidate_election_stats_official AS " +
                "SELECT election_id, candidate_id, candidate_name, party_id, party_name, abbreviation, SUM(candidate_votes) AS candidate_votes, SUM(ballots_cast) AS ballots_cast, SUM(center_valid_votes) AS total_valid_votes, SUM(center_invalid_total) AS total_invalid_votes, fn_pct(SUM(candidate_votes)::numeric, NULLIF(SUM(center_valid_votes),0)::numeric) AS vote_share_pct FROM v_candidate_center_stats_official GROUP BY election_id, candidate_id, candidate_name, party_id, party_name, abbreviation;");

        // seed: two centers for one candidate across the election
        electionId = UUID.randomUUID();
        county = UUID.randomUUID();
        district = UUID.randomUUID();
        center1 = UUID.randomUUID();
        center2 = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        partyId = UUID.randomUUID();

        jdbc.update("INSERT INTO county(county_id, county_name) VALUES (?, ?)", county, "County E");
        jdbc.update("INSERT INTO district(district_id, district_name, county_id) VALUES (?, ?, ?)", district, "District E", county);
        jdbc.update("INSERT INTO polling_center(center_id, district_id, code, center_name) VALUES (?, ?, ?, ?)", center1, district, "CE-1", "Center E1");
        jdbc.update("INSERT INTO polling_center(center_id, district_id, code, center_name) VALUES (?, ?, ?, ?)", center2, district, "CE-2", "Center E2");

        jdbc.update("INSERT INTO polling_center_allocation(org_id, election_id, center_id, registered_voters, ballots_cast, valid_votes, invalid_total) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), electionId, center1, 1000, 600, 580, 20);
        jdbc.update("INSERT INTO polling_center_allocation(org_id, election_id, center_id, registered_voters, ballots_cast, valid_votes, invalid_total) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), electionId, center2, 800, 500, 480, 20);

        jdbc.update("INSERT INTO candidate(candidate_id, full_name, party_id) VALUES (?, ?, ?)", candidateId, "Election Candidate", partyId);
        jdbc.update("INSERT INTO party(party_id, party_name, abbreviation) VALUES (?, ?, ?)", partyId, "Election Party", "ELT");

        String json1 = String.format("{\"%s\":\"600\"}", candidateId.toString());
        String json2 = String.format("{\"%s\":\"400\"}", candidateId.toString());
        jdbc.update("INSERT INTO nec_result(org_id, election_id, center_id, ballots_cast, candidate_votes, invalid_ballots, blank_ballots, rejected_ballots, spoiled_ballots, is_published) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), electionId, center1, 600, json1, 10, 5, 3, 2, true);
        jdbc.update("INSERT INTO nec_result(org_id, election_id, center_id, ballots_cast, candidate_votes, invalid_ballots, blank_ballots, rejected_ballots, spoiled_ballots, is_published) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), electionId, center2, 500, json2, 10, 5, 3, 2, true);

        var rows = jdbc.queryForList("SELECT * FROM v_candidate_election_stats_official");
        assertThat(rows).isNotEmpty();
    }

    @Test
    @Transactional
    void candidateElectionOfficialAggregation_returnsExpectedValues_and_cacheEviction() {
        tenantGucService.applyForTransaction((java.util.UUID) null, false, false);

        var page = service.listCandidateElectionOfficialStats(electionId, null, null, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);

        CandidateElectionStatsOfficialDto dto = page.getContent().get(0);
        // candidate_votes = 600 + 400 = 1000
        assertThat(dto.getCandidateVotes()).isEqualTo(1000);
        // total_valid_votes = 580 + 480 = 1060
        assertThat(dto.getTotalValidVotes()).isEqualTo(1060);
        BigDecimal expectedShare = BigDecimal.valueOf(1000d).divide(BigDecimal.valueOf(1060d), 6, BigDecimal.ROUND_HALF_UP);
        assertThat(dto.getVoteSharePct()).isEqualByComparingTo(expectedShare);

        cacheEvictService.evictByElection(electionId);
    }
}