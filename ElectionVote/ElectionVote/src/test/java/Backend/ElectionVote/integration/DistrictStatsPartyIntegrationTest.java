package Backend.ElectionVote.integration;

import Backend.ElectionVote.views.repo.DistrictStatsPartyRepository;
import Backend.ElectionVote.views.service.ElectionValidationService;
import Backend.ElectionVote.views.service.TenantGucService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies Spring wiring for view-related beans and that
 * TenantGucService can be invoked inside a transaction.
 *
 * Notes:
 * - Place this test under the same base package as your application (Backend.ElectionVote.*)
 *   so @SpringBootTest can component-scan your application automatically.
 * - This test starts a Testcontainers Postgres instance and sets the Spring datasource
 *   properties dynamically so Spring Boot will use the container DB.
 * - The test purpose here is to ensure beans are autowired correctly and that the
 *   TenantGucService applies transaction-local settings without throwing when run
 *   inside a transactional test method. It intentionally does not require the full
 *   view/table schema to be present (so it is safe as a wiring sanity check).
 *
 * If you want a full end-to-end test that exercises the repository and view SQL,
 * add Flyway migrations for the test profile or use JdbcTemplate to create the
 * necessary tables/views before running assertions.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DistrictStatsPartyIntegrationTest {

    // Testcontainers Postgres; you can change image/tag as needed
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        String url = postgres.getJdbcUrl();
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Disable Flyway migration here if your tests don't include migrations, or enable if you have test migrations
        // registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private TenantGucService tenantGucService;

    @Autowired
    private DistrictStatsPartyRepository repo;

    @Autowired
    private ElectionValidationService electionValidationService;

    @BeforeAll
    public void beforeAll() {
        // Optional: additional test setup if you want to apply migrations or seed data via JdbcTemplate.
    }

    @AfterAll
    public void afterAll() {
        // Stop container when tests are done
        postgres.stop();
    }

    @Test
    @Transactional
    public void wiringAndTenantGucApply_shouldWorkInsideTransaction() {
        // Basic bean wiring assertions
        assertThat(tenantGucService).as("TenantGucService should be autowired").isNotNull();
        assertThat(repo).as("DistrictStatsPartyRepository should be autowired").isNotNull();
        assertThat(electionValidationService).as("ElectionValidationService should be autowired").isNotNull();

        // Apply tenant GUCs inside an active transaction (the @Transactional annotation ensures a transaction)
        UUID orgId = UUID.randomUUID();
        // This should not throw IllegalStateException (must be called inside a transaction)
        tenantGucService.applyForTransaction(orgId, false, false);

        // If you want to exercise the repository against real views/tables, ensure migrations run in the test profile
        // and then call repo.findAll(PageRequest.of(0, 10)) and assert expected results.
    }
}