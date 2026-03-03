
package Backend.ElectionVote.integration;

import Backend.ElectionVote.dto.VoteSubmissionVerifyRequest;
import Backend.ElectionVote.entity.*;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.service.VoteSubmissionService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: verifies submit -> verify -> RecomputeEvent -> vote_tally population and retry behavior.
 *
 * Requirements:
 * - Testcontainers + Postgres on test classpath
 * - Flyway migrations available under src/main/resources/db/migration (runs on startup)
 * - RecomputeListener, VoteTallyService, and VoteSubmissionService wired in Spring context
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecomputeIntegrationTest {

    @Container
    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:14-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Ensure Flyway runs in tests if your app is configured to run migrations on startup
        registry.add("spring.flyway.enabled", () -> "true");

        // Optionally tune recompute backoff in test profile (your app must read this property)
        registry.add("app.recompute.base-delay-ms", () -> "500");
    }

    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private ElectionRepository electionRepository;
    @Autowired private CandidateRepository candidateRepository;
    @Autowired private PollingCenterRepository pollingCenterRepository;
    @Autowired private PollingPlaceRepository pollingPlaceRepository;
    @Autowired private SystemUserRepository systemUserRepository;
    @Autowired private VoteSubmissionRepository voteSubmissionRepository;
    @Autowired private VoteTallyRepository voteTallyRepository; // optional, jdbc used for assertions
    @Autowired private VoteSubmissionService voteSubmissionService;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Organization org;
    private Election election;
    private Candidate candidate;
    private PollingCenter center;
    private PollingPlace place;
    private SystemUser agent;
    private SystemUser verifier;

    @BeforeAll
    void setupEntities() {
        org = new Organization(); org.setOrgName("Test Org"); org = organizationRepository.save(org);

        election = new Election(); election.setElectionName("Test Election"); election.setYear(LocalDateTime.now().getYear()); election = electionRepository.save(election);

        center = new PollingCenter(); center.setCenterName("Test Center"); center = pollingCenterRepository.save(center);

        place = new PollingPlace(); place.setLabel("Test Place"); place.setPollingCenter(center); place = pollingPlaceRepository.save(place);

        candidate = new Candidate(); candidate.setFullName("Alice"); candidate = candidateRepository.save(candidate);

        agent = new SystemUser(); agent.setFirstName("Agent"); agent.setLastName("User"); agent = systemUserRepository.save(agent);

        verifier = new SystemUser(); verifier.setFirstName("Verifier"); verifier.setLastName("User"); verifier = systemUserRepository.save(verifier);
    }

    @AfterAll
    void cleanup() {
        // optional cleanup
    }

    @Test
    void verify_and_recompute_populates_vote_tally() {
        VoteSubmission submission = new VoteSubmission();
        submission.setOrganization(org);
        submission.setElection(election);
        submission.setPollingCenter(center);
        submission.setPollingPlace(place);
        submission.setAgent(agent);
        submission.setCandidateVotes(Map.of(candidate.getCandidateId(), 1));
        submission.setBallotsCast(1);
        submission = voteSubmissionRepository.save(submission);

        VoteSubmissionVerifyRequest req = new VoteSubmissionVerifyRequest();
        req.setVerifierUserId(verifier.getUserId());
        req.setAccept(true);
        req.setComment("ok");

        voteSubmissionService.verify(submission.getSubmissionId(), req);

        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    Long cnt = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM vote_tally WHERE org_id = ? AND election_id = ?",
                            Long.class, org.getOrgId(), election.getElectionId());
                    assertThat(cnt).isNotNull().isGreaterThan(0);
                });
    }

    @Test
    void recompute_retries_when_advisory_lock_held_then_succeeds() throws Exception {
        VoteSubmission submission = new VoteSubmission();
        submission.setOrganization(org);
        submission.setElection(election);
        submission.setPollingCenter(center);
        submission.setPollingPlace(place);
        submission.setAgent(agent);
        submission.setCandidateVotes(Map.of(candidate.getCandidateId(), 2));
        submission.setBallotsCast(2);
        submission = voteSubmissionRepository.save(submission);

        long advisoryKey = computeAdvisoryKey(org.getOrgId(), election.getElectionId());

        // Acquire a dedicated connection and hold the advisory lock so the recompute can't get it
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT pg_advisory_lock(?)")) {
                ps.setLong(1, advisoryKey);
                ps.executeQuery(); // holds the lock until connection closed

                VoteSubmissionVerifyRequest req = new VoteSubmissionVerifyRequest();
                req.setVerifierUserId(verifier.getUserId());
                req.setAccept(true);
                req.setComment("ok with lock held");
                voteSubmissionService.verify(submission.getSubmissionId(), req);

                // allow time for initial attempt and scheduling of retry
                Thread.sleep(1000);
            }
        }

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Long cnt = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM vote_tally WHERE org_id = ? AND election_id = ?",
                            Long.class, org.getOrgId(), election.getElectionId());
                    assertThat(cnt).isNotNull().isGreaterThan(0);
                });
    }

    // Replicate computeAdvisoryKey logic used by the service so test and service use identical lock id
    private long computeAdvisoryKey(UUID orgId, UUID electionId) {
        UUID combined = UUID.nameUUIDFromBytes((orgId.toString() + "|" + electionId.toString()).getBytes(StandardCharsets.UTF_8));
        return combined.getMostSignificantBits() ^ combined.getLeastSignificantBits();
    }
}


//package Backend.ElectionVote.integration;
//
//import Backend.ElectionVote.entity.*;
//import Backend.ElectionVote.repository.*;
//import Backend.ElectionVote.service.VoteSubmissionService;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import javax.sql.DataSource;
//import java.nio.charset.StandardCharsets;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.Map;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//
///**
// * Integration test: verifies the end-to-end flow submit -> verify -> RecomputeEvent -> vote_tally population.
// *
// * Requirements:
// * - Testcontainers + Postgres
// * - Application uses Flyway on startup to create schema/migrations
// * - RecomputeListener and VoteTallyService are wired in the Spring context
// *
// * This test:
// *  - creates organization, election, candidate, polling center/place and users
// *  - creates a PENDING VoteSubmission and then calls verify(...) on the service
// *  - waits for the async recompute to populate vote_tally
// *  - then simulates advisory-lock contention by holding the advisory lock on a dedicated connection,
// *    triggers another verify, then releases the lock and asserts retries eventually populate tallies
// */
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Testcontainers
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class RecomputeIntegrationTest {
//
//
//    @Container
//    public static final PostgreSQLContainer<?> POSTGRES =
//            new PostgreSQLContainer<>("postgres:14-alpine")
//                    .withDatabaseName("testdb")
//                    .withUsername("test")
//                    .withPassword("test");
//
//    @DynamicPropertySource
//    static void datasourceProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
//        registry.add("spring.datasource.username", POSTGRES::getUsername);
//        registry.add("spring.datasource.password", POSTGRES::getPassword);
//
//        // Ensure Flyway runs in tests if your app is configured to run migrations on startup
//        registry.add("spring.flyway.enabled", () -> "true");
//
//        // Reduce any long timeouts in test profile if you have them (optional)
//        registry.add("app.recompute.base-delay-ms", () -> "1000");
//    }
//
//    @Autowired
//    private OrganizationRepository organizationRepository;
//
//    @Autowired
//    private ElectionRepository electionRepository;
//
//    @Autowired
//    private CandidateRepository candidateRepository;
//
//    @Autowired
//    private PollingCenterRepository pollingCenterRepository;
//
//    @Autowired
//    private PollingPlaceRepository pollingPlaceRepository;
//
//    @Autowired
//    private SystemUserRepository systemUserRepository;
//
//    @Autowired
//    private VoteSubmissionRepository voteSubmissionRepository;
//
//    @Autowired
//    private VoteTallyRepository voteTallyRepository;
//
//    @Autowired
//    private VoteSubmissionService voteSubmissionService;
//
//    @Autowired
//    private DataSource dataSource;
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//
//    private Organization org;
//    private Election election;
//    private Candidate candidate;
//    private PollingCenter center;
//    private PollingPlace place;
//    private SystemUser agent;
//    private SystemUser verifier;
//
//    @BeforeAll
//    void setupEntities() {
//        // create organization
//        org = new Organization();
//        org.setOrgName("Test Org");
//        org = organizationRepository.save(org);
//
//        // create election
//        election = new Election();
//        election.setElectionName("Test Election");
//        election.setYear(LocalDateTime.now().getYear());
//        election = electionRepository.save(election);
//
//        // create polling center and place
//        center = new PollingCenter();
//        center.setCenterName("Test Center");
//        center = pollingCenterRepository.save(center);
//
//        place = new PollingPlace();
//        place.setLabel("Test Place");
//        place.setPollingCenter(center);
//        place = pollingPlaceRepository.save(place);
//
//        // create candidate
//        candidate = new Candidate();
//        candidate.setFullName("Alice");
//        candidate = candidateRepository.save(candidate);
//
//        // create agent and verifier users
//        agent = new SystemUser();
//        agent.setFirstName("Agent");
//        agent.setLastName("User");
//        agent = systemUserRepository.save(agent);
//
//        verifier = new SystemUser();
//        verifier.setFirstName("Verifier");
//        verifier.setLastName("User");
//        verifier = systemUserRepository.save(verifier);
//
//    }
//
//    @AfterAll
//    void cleanup() {
//        // cleanup DB if needed
//    }
//
//    @Test
//    void verify_and_recompute_populates_vote_tally() {
//        // create submission (PENDING)
//        VoteSubmission submission = new VoteSubmission();
//        submission.setOrganization(org);
//        submission.setElection(election);
//        submission.setPollingCenter(center);
//        submission.setPollingPlace(place);
//        submission.setAgent(agent);
//        submission.setCandidateVotes(Map.of(candidate.getCandidateId(), 1));
//        submission.setBallotsCast(1);
//        // status defaults to PENDING in the entity
//        submission = voteSubmissionRepository.save(submission);
//
//        // verify via service
//        Backend.ElectionVote.dto.VoteSubmissionVerifyRequest req = new Backend.ElectionVote.dto.VoteSubmissionVerifyRequest();
//        req.setVerifierUserId(verifier.getUserId());
//        req.setAccept(true);
//        req.setComment("ok");
//
//        voteSubmissionService.verify(submission.getSubmissionId(), req);
//
//        // wait for the async recompute to populate vote_tally rows
//        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(250)).untilAsserted(() ->
//                assertThat(voteTallyRepository.findByOrganization_OrgIdAndElection_ElectionId(org.getOrgId(), election.getElectionId()))
//                        .isNotEmpty()
//        );
//    }
//
//    @Test
//    void recompute_retries_when_advisory_lock_held_then_succeeds() throws Exception {
//        // create another submission and ensure there are no tallies initially (we will verify again)
//        VoteSubmission submission = new VoteSubmission();
//        submission.setOrganization(org);
//        submission.setElection(election);
//        submission.setPollingCenter(center);
//        submission.setPollingPlace(place);
//        submission.setAgent(agent);
//        submission.setCandidateVotes(Map.of(candidate.getCandidateId(), 2));
//        submission.setBallotsCast(2);
//        submission = voteSubmissionRepository.save(submission);
//
//        // compute advisory key the same way service does (name-based UUID then MSB^LSB)
//        long advisoryKey = computeAdvisoryKey(org.getOrgId(), election.getElectionId());
//
//        // Acquire a dedicated connection and hold the advisory lock so the recompute can't get it
//        try (Connection conn = dataSource.getConnection()) {
//            // Use a prepared statement and keep the connection open to hold the lock
//            try (PreparedStatement ps = conn.prepareStatement("SELECT pg_advisory_lock(?)")) {
//                ps.setLong(1, advisoryKey);
//                ps.executeQuery(); // lock acquired and held while connection open
//
//                // Call verify (recompute will be triggered but cannot obtain advisory lock)
//                Backend.ElectionVote.dto.VoteSubmissionVerifyRequest req = new Backend.ElectionVote.dto.VoteSubmissionVerifyRequest();
//                req.setVerifierUserId(verifier.getUserId());
//                req.setAccept(true);
//                req.setComment("ok with lock held");
//                voteSubmissionService.verify(submission.getSubmissionId(), req);
//
//                // Wait some time to allow the first attempt to fail and scheduler to schedule a retry
//                // (We expect the retry scheduler to run after base delay)
//                Thread.sleep(2500);
//
//                // Now release the lock by closing the connection (try-with-resources will close at end of try)
//                // After closing the connection the scheduled retry should be able to acquire the advisory lock and run.
//            }
//        }
//
//        // Wait for eventual success (longer timeout because retries/backoff)
//        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500)).untilAsserted(() ->
//                assertThat(voteTallyRepository.findByOrganization_OrgIdAndElection_ElectionId(org.getOrgId(), election.getElectionId()))
//                        .isNotEmpty()
//        );
//    }
//
//    // Replicate computeAdvisoryKey logic used by the service so test and service use identical lock id
//    private long computeAdvisoryKey(UUID orgId, UUID electionId) {
//        UUID combined = UUID.nameUUIDFromBytes((orgId.toString() + "|" + electionId.toString()).getBytes(StandardCharsets.UTF_8));
//        return combined.getMostSignificantBits() ^ combined.getLeastSignificantBits();
//    }
//}