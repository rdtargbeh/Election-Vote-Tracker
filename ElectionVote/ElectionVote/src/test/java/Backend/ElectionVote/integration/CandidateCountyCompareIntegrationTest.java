package Backend.ElectionVote.integration;


import Backend.ElectionVote.views.dto.CandidateCountyCompareDto;
import Backend.ElectionVote.views.service.CandidateCountyCompareService;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for v_candidate_county_compare.
 * Builds minimal v_candidate_county_stats_party and v_candidate_county_stats_official and compares values.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CandidateCountyCompareIntegrationTest {

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
    private CandidateCountyCompareService service;

    @Autowired
    private TenantGucService tenantGucService;

    @Autowired
    private StatsCacheEvictService cacheEvictService;

    private UUID electionId;
    private UUID countyId;
    private UUID candidateId;
    private UUID partyOrgId;

    @BeforeAll
    void setupSchemaAndData() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS county (county_id uuid PRIMARY KEY, county_name text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS candidate (candidate_id uuid PRIMARY KEY, full_name text, party_id uuid);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS party (party_id uuid PRIMARY KEY, party_name text, abbreviation text);");

        // create party view (minimal) and official view tables used by compare view
        // We'll create the two underlying views as simple tables with the same columns for test simplicity
        jdbc.execute("CREATE TABLE IF NOT EXISTS v_candidate_county_stats_party (org_id uuid, election_id uuid, county_id uuid, county_name text, candidate_id uuid, candidate_name text, party_id uuid, party_name text, abbreviation text, candidate_votes int, ballots_cast int, total_valid_votes int, total_invalid_votes int, vote_share_pct numeric);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS v_candidate_county_stats_official (election_id uuid, county_id uuid, county_name text, candidate_id uuid, candidate_name text, party_id uuid, party_name text, abbreviation text, candidate_votes int, ballots_cast int, total_valid_votes int, total_invalid_votes int, vote_share_pct numeric);");

        jdbc.execute("CREATE OR REPLACE VIEW v_candidate_county_compare AS " +
                "SELECT p.org_id, COALESCE(p.election_id, o.election_id) AS election_id, COALESCE(p.county_id, o.county_id) AS county_id, COALESCE(p.county_name, o.county_name) AS county_name, COALESCE(p.candidate_id, o.candidate_id) AS candidate_id, COALESCE(p.candidate_name, o.candidate_name) AS candidate_name, COALESCE(p.party_id, o.party_id) AS party_id, COALESCE(p.party_name, o.party_name) AS party_name, COALESCE(p.abbreviation, o.abbreviation) AS party_code, p.candidate_votes AS party_candidate_votes, o.candidate_votes AS official_candidate_votes, (COALESCE(p.candidate_votes,0) - COALESCE(o.candidate_votes,0)) AS diff_votes, p.vote_share_pct AS party_vote_share_pct, o.vote_share_pct AS official_vote_share_pct FROM v_candidate_county_stats_party p FULL OUTER JOIN v_candidate_county_stats_official o ON p.election_id = o.election_id AND p.county_id = o.county_id AND p.candidate_id = o.candidate_id;");

        // seed a party row and official row with a difference
        electionId = UUID.randomUUID();
        countyId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        partyOrgId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        jdbc.update("INSERT INTO county(county_id, county_name) VALUES (?, ?)", countyId, "Compare County");
        jdbc.update("INSERT INTO candidate(candidate_id, full_name, party_id) VALUES (?, ?, ?)", candidateId, "Compare Candidate", partyId);
        jdbc.update("INSERT INTO party(party_id, party_name, abbreviation) VALUES (?, ?, ?)", partyId, "Compare Party", "CMP");

        // party submission: 300 votes
        jdbc.update("INSERT INTO v_candidate_county_stats_party(org_id, election_id, county_id, county_name, candidate_id, candidate_name, party_id, party_name, abbreviation, candidate_votes, vote_share_pct) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                partyOrgId, electionId, countyId, "Compare County", candidateId, "Compare Candidate", partyId, "Compare Party", "CMP", 300, 0.3);

        // official: 250 votes
        jdbc.update("INSERT INTO v_candidate_county_stats_official(election_id, county_id, county_name, candidate_id, candidate_name, party_id, party_name, abbreviation, candidate_votes, vote_share_pct) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                electionId, countyId, "Compare County", candidateId, "Compare Candidate", partyId, "Compare Party", "CMP", 250, 0.25);

        var rows = jdbc.queryForList("SELECT * FROM v_candidate_county_compare");
        assertThat(rows).isNotEmpty();
    }

    @Test
    @Transactional
    void compareAggregation_returnsExpectedValues() {
        tenantGucService.applyForTransaction((java.util.UUID) null, false, false);

        var page = service.listCandidateCountyCompare(electionId, null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);

        CandidateCountyCompareDto dto = page.getContent().get(0);
        assertThat(dto.getPartyCandidateVotes()).isEqualTo(300);
        assertThat(dto.getOfficialCandidateVotes()).isEqualTo(250);
        assertThat(dto.getDiffVotes()).isEqualTo(50);

        // clear caches
        cacheEvictService.evictByElection(electionId);
    }
}