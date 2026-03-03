package Backend.ElectionVote.integration;


import Backend.ElectionVote.views.repo.CountyStatsPartyRepository;
import Backend.ElectionVote.views.service.ElectionValidationService;
import Backend.ElectionVote.views.service.TenantGucService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test skeleton for county-level stats.
 * Place under src/test/java and ensure test profile runs migrations for full end-to-end checks.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CountyStatsPartyIntegrationTest {

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
    }

    @Autowired
    private TenantGucService tenantGucService;

    @Autowired
    private CountyStatsPartyRepository repo;

    @Autowired
    private ElectionValidationService electionValidationService;

    @BeforeAll
    void beforeAll() {
        // Optional: run test-specific migrations or seed minimal data
    }

    @AfterAll
    void afterAll() {
        postgres.stop();
    }

    @Test
    @Transactional
    void wiringAndTenantGucApply_shouldWorkInsideTransaction() {
        assertThat(tenantGucService).isNotNull();
        assertThat(repo).isNotNull();
        assertThat(electionValidationService).isNotNull();

        UUID orgId = UUID.randomUUID();
        tenantGucService.applyForTransaction(orgId, false, false);

        var page = repo.findAll();
        assertThat(page).isNotNull();
    }
}