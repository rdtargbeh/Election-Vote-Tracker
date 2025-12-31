package Backend.ElectionVote.integration;

import Backend.ElectionVote.views.dto.NecResultGeoDto;
import Backend.ElectionVote.views.service.NecResultGeoService;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for v_nec_result_geo / mv_nec_result_geo.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NecResultGeoIntegrationTest {

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
    private NecResultGeoService service;

    @Autowired
    private TenantGucService tenantGucService;

    @Autowired
    private StatsCacheEvictService cacheEvictService;

    private UUID electionId;
    private UUID centerId;
    private UUID districtId;
    private UUID countyId;

    @BeforeAll
    void setupSchemaAndData() {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto;");
        jdbc.execute("CREATE TABLE IF NOT EXISTS county (county_id uuid PRIMARY KEY, county_name text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS district (district_id uuid PRIMARY KEY, district_name text, county_id uuid);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS polling_center (center_id uuid PRIMARY KEY, district_id uuid, code text, center_name text);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS polling_center_allocation (allocation_id serial PRIMARY KEY, org_id uuid, election_id uuid, center_id uuid, registered_voters int, ballots_issued int, ballots_cast int, valid_votes int, invalid_total int);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS nec_result (result_id serial PRIMARY KEY, org_id uuid, election_id uuid, center_id uuid, candidate_votes jsonb, total_registered_voters int, ballots_cast int, invalid_ballots int, blank_ballots int, rejected_ballots int, spoiled_ballots int, is_published boolean, source text, upload_time timestamptz);");

        jdbc.execute("CREATE OR REPLACE VIEW v_nec_result_geo AS " +
                "SELECT nr.result_id, nr.election_id, pc.center_id, pc.code AS center_code, pc.center_name AS center_name, d.district_id, d.district_name, c.county_id, c.county_name, nr.candidate_votes, nr.total_registered_voters, nr.ballots_cast, nr.invalid_ballots, nr.blank_ballots, nr.rejected_ballots, nr.spoiled_ballots, pca.ballots_issued, nr.source, nr.upload_time " +
                "FROM nec_result nr JOIN polling_center pc ON pc.center_id = nr.center_id JOIN district d ON d.district_id = pc.district_id JOIN county c ON c.county_id = d.county_id LEFT JOIN polling_center_allocation pca ON pca.center_id = nr.center_id AND pca.election_id = nr.election_id WHERE nr.is_published = TRUE;");

        jdbc.execute("DROP MATERIALIZED VIEW IF EXISTS mv_nec_result_geo;");
        jdbc.execute("CREATE MATERIALIZED VIEW mv_nec_result_geo AS SELECT * FROM v_nec_result_geo;");

        // seed one published nec_result row
        electionId = UUID.randomUUID();
        countyId = UUID.randomUUID();
        districtId = UUID.randomUUID();
        centerId = UUID.randomUUID();

        jdbc.update("INSERT INTO county(county_id, county_name) VALUES (?, ?)", countyId, "Geo County");
        jdbc.update("INSERT INTO district(district_id, district_name, county_id) VALUES (?, ?, ?)", districtId, "Geo District", countyId);
        jdbc.update("INSERT INTO polling_center(center_id, district_id, code, center_name) VALUES (?, ?, ?, ?)", centerId, districtId, "G-1", "Geo Center");

        jdbc.update("INSERT INTO polling_center_allocation(org_id, election_id, center_id, registered_voters, ballots_issued, ballots_cast, valid_votes, invalid_total) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), electionId, centerId, 1000, 650, 630, 600, 30);

        String candidateJson = "{\"" + UUID.randomUUID() + "\":\"350\",\"" + UUID.randomUUID() + "\":\"250\"}";
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.update("INSERT INTO nec_result(org_id, election_id, center_id, candidate_votes, total_registered_voters, ballots_cast, invalid_ballots, blank_ballots, rejected_ballots, spoiled_ballots, is_published, source, upload_time) VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), electionId, centerId, candidateJson, 1000, 630, 20, 5, 3, 2, true, "upload", now);

        // refresh materialized view in test DB so mv has rows
        jdbc.execute("REFRESH MATERIALIZED VIEW mv_nec_result_geo;");

        var rows = jdbc.queryForList("SELECT * FROM mv_nec_result_geo");
        assertThat(rows).isNotEmpty();
    }

    @Test
    @Transactional
    void necGeo_returnsExpectedRow_and_cacheEviction() {
        // anonymous context
        tenantGucService.applyForTransaction((java.util.UUID) null, false, false);

        var page = service.listNecResultGeo(electionId, null, null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);

        NecResultGeoDto dto = page.getContent().get(0);
        assertThat(dto.getBallotsCast()).isEqualTo(630);
        assertThat(dto.getTotalRegisteredVoters()).isEqualTo(1000);
        assertThat(dto.getBallotsIssued()).isEqualTo(650);
        assertThat(dto.getCandidateVotes()).isNotNull();

        // targeted eviction by election after publish
        cacheEvictService.evictByElection(electionId);
    }
}
