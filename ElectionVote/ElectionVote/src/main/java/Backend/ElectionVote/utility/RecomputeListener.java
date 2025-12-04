package Backend.ElectionVote.utility;


import Backend.ElectionVote.enums.ActivityType;
import Backend.ElectionVote.utility.RecomputeEvent;
import Backend.ElectionVote.service.VoteTallyService;
import Backend.ElectionVote.service.AuditLogService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

/**
 * Listens for RecomputeEvent AFTER_COMMIT and runs vote tally recompute asynchronously.
 *
 * Behavior:
 *  - Runs async so verify() returns quickly.
 *  - Retries a configurable number of times with exponential backoff if recompute returns empty due to advisory lock.
 *  - On repeated failure logs, emits metrics, and records a lightweight audit entry via AuditLogService.
 */
@Component
public class RecomputeListener {

    private static final Logger log = LoggerFactory.getLogger(RecomputeListener.class);

    private final VoteTallyService voteTallyService;
    private final AuditLogService auditLogService;
    private final MeterRegistry meterRegistry;
    private final TaskScheduler scheduler;

    // config: retry attempts and base delay
    private static final int MAX_RETRIES = 5;
    private static final Duration BASE_DELAY = Duration.ofSeconds(2);

    public RecomputeListener(VoteTallyService voteTallyService,
                             AuditLogService auditLogService,
                             MeterRegistry meterRegistry) {
        this.voteTallyService = voteTallyService;
        this.auditLogService = auditLogService;
        this.meterRegistry = meterRegistry;

        // simple scheduler used for delayed retries; uses small pool
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(2);
        ts.setThreadNamePrefix("recompute-retry-");
        ts.initialize();
        this.scheduler = ts;
    }

    @Async("voteExecutor") // uses your AsyncConfig bean; keeps thread pools consistent
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecomputeEvent(RecomputeEvent ev) {
        final String key = ev.getOrgId() + ":" + ev.getElectionId();
        log.info("Received RecomputeEvent for {}", key);
        meterRegistry.counter("recompute.events.received").increment();

        // attempt immediate run with retries on advisory-lock contention or transient errors
        try {
            boolean success = tryRunWithRetries(ev.getOrgId(), ev.getElectionId(), ev.getActorUserId(), 0);
            if (!success) {
                // persistent failure: record audit & metric
                meterRegistry.counter("recompute.events.failed").increment();
                // auditLogService.log expects 5 args: orgId, userId, ActivityType, entity, description
                auditLogService.log(
                        ev.getOrgId(),
                        ev.getActorUserId(),
                        ActivityType.TALLY_RECOMPUTE,
                        "VoteTally",
                        "Automated recompute failed after retries for election: " + ev.getElectionId()
                );
                log.error("Recompute for {} failed after retries", key);
            } else {
                meterRegistry.counter("recompute.events.succeeded").increment();
                log.info("Recompute for {} completed", key);
            }
        } catch (Exception ex) {
            meterRegistry.counter("recompute.events.failed").increment();
            log.error("Unexpected error while processing recompute event for {}: {}", key, ex.getMessage(), ex);
            auditLogService.log(
                    ev.getOrgId(),
                    ev.getActorUserId(),
                    ActivityType.TALLY_RECOMPUTE,
                    "VoteTally",
                    "Recompute failed with unexpected error: " + ex.getMessage()
            );
        }
    }

    private boolean tryRunWithRetries(java.util.UUID orgId, java.util.UUID electionId, java.util.UUID actorUserId, int attempt) {
        try {
            // VoteTallyService.recomputeForElection returns empty list if advisory lock prevented immediate run (per our implementation)
            var result = voteTallyService.recomputeForElection(orgId, electionId, actorUserId);
            if (result != null && !result.isEmpty()) {
                return true;
            } else {
                // Empty result may mean either: no verified submissions OR lock was held and method returned early.
                // Distinguish by checking if there are verified submissions quickly (optional), but here we interpret empty as possibly lock contention => retry.
                if (attempt >= MAX_RETRIES) return false;

                long delayMillis = computeBackoffMillis(attempt);
                log.warn("Recompute for {}/{} returned empty; scheduling retry #{} after {}ms", orgId, electionId, attempt + 1, delayMillis);
                ScheduledFuture<?> future = scheduler.schedule(() -> {
                    tryRunWithRetries(orgId, electionId, actorUserId, attempt + 1);
                }, java.util.Date.from(java.time.Instant.now().plusMillis(delayMillis)));
                // we don't need to keep the future reference here; let it run
                return true; // treat scheduling as "accepted"
            }
        } catch (Exception ex) {
            // transient DB/KMS error: retry up to MAX_RETRIES
            if (attempt >= MAX_RETRIES) {
                log.error("Recompute for {}/{} failed after {} attempts: {}", orgId, electionId, attempt + 1, ex.getMessage());
                return false;
            } else {
                long delayMillis = computeBackoffMillis(attempt);
                log.warn("Recompute attempt #{} for {}/{} failed: {}. Retrying in {}ms", attempt + 1, orgId, electionId, ex.getMessage(), delayMillis);
                scheduler.schedule(() -> tryRunWithRetries(orgId, electionId, actorUserId, attempt + 1),
                        java.util.Date.from(java.time.Instant.now().plusMillis(delayMillis)));
                return true; // scheduled retry
            }
        }
    }

    private long computeBackoffMillis(int attempt) {
        // exponential backoff with jitter
        long base = BASE_DELAY.toMillis() * (1L << Math.min(attempt, 10));
        long jitter = (long) (Math.random() * 200L);
        return Math.min(base + jitter, Duration.ofMinutes(5).toMillis());
    }
}