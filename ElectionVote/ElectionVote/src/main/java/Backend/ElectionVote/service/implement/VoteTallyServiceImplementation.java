package Backend.ElectionVote.service.implement;


import Backend.ElectionVote.dto.VoteTallyDto;
import Backend.ElectionVote.entity.*;

import Backend.ElectionVote.enums.VoteStatus;
import Backend.ElectionVote.mapper.VoteTallyMapper;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.service.AdvisoryLockNotAcquiredException;
import Backend.ElectionVote.service.VoteTallyService;
import Backend.ElectionVote.utility.VoteTallySpecs;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * VoteTallyService implementation.
 *
 * Key behavioral changes:
 *  - If the Postgres advisory lock cannot be acquired (pg_try_advisory_lock -> false),
 *    the method throws AdvisoryLockNotAcquiredException so callers/listeners can schedule retries.
 *  - Computation of tallies happens before deletes/inserts; delete/insert happen within the same transaction.
 */


@Service
@RequiredArgsConstructor
public class VoteTallyServiceImplementation implements VoteTallyService {


    private static final Logger log = LoggerFactory.getLogger(VoteTallyServiceImplementation.class);

    private final VoteTallyRepository voteTallyRepository;
    private final OrganizationRepository orgRepo;
    private final VoteSubmissionRepository voteSubmissionRepository;
    private final PartyRepository partyRepo;
    private final CandidateRepository candidateRepo;
    private final SystemUserRepository systemUserRepo;
    private final ElectionRepository electionRepo;
    private final JdbcTemplate jdbc;

    private final VoteTallyMapper mapper = new VoteTallyMapper();


    // In-JVM guard to prevent concurrent recomputes for same org+election
    // NOTE: for multi-node deployments we also acquire a Postgres advisory lock.
    private static final ConcurrentHashMap<String, Object> RUN_LOCKS = new ConcurrentHashMap<>();



    @Override
    @Transactional(readOnly = true)
    public Page<VoteTallyDto> search(UUID orgId,
                                     UUID electionId,
                                     UUID candidateId,
                                     UUID partyId,
                                     Pageable pageable) {

        Specification<VoteTally> spec = Specification
                .where(VoteTallySpecs.orgEquals(orgId))
                .and(VoteTallySpecs.electionEquals(electionId))
                .and(VoteTallySpecs.candidateEquals(candidateId))
                .and(VoteTallySpecs.partyEquals(partyId));

        return voteTallyRepository.findAll(spec, pageable).map(mapper::toDTO);
    }

    /**
     * Convenience overload – system recompute (no explicit user).
     */
    @Override
    @Transactional
    public List<VoteTallyDto> recomputeForElection(UUID orgId, UUID electionId) {
        return recomputeForElection(orgId, electionId, null);
    }


    /**
     * Recompute aggregated vote totals for an election using VERIFIED submissions only.
     *
     * This method:
     *  - acquires an in-JVM guard and a Postgres advisory lock (pg_try_advisory_lock) to avoid concurrent runs
     *  - performs a set-based aggregation in the DB from vote_submission.candidate_votes (jsonb)
     *  - deletes existing tallies for the org+election and inserts new tallies in a single transaction
     *
     * Note:
     *  - If the advisory lock cannot be acquired because another node is running a recompute,
     *    this call returns an empty list and logs a warning.
     *  - The SQL filters candidate_votes values that are not strictly numeric to avoid runtime cast exceptions.
     */
    @Override
    @Transactional
    public List<VoteTallyDto> recomputeForElection(UUID orgId,
                                                   UUID electionId,
                                                   UUID recomputedByUserId) {

        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));

        // JVM-level guard
        String lockKey = orgId.toString() + ":" + electionId.toString();
        Object myLock = new Object();
        Object existing = RUN_LOCKS.putIfAbsent(lockKey, myLock);
        if (existing != null) {
            log.warn("Recompute for org={} election={} already in progress (in-JVM). Skipping duplicate request.", orgId, electionId);
            return List.of();
        }

        boolean advisoryLockAcquired = false;
        long advisoryKey = computeAdvisoryKey(orgId, electionId);

        try {
            // Acquire Postgres advisory lock (non-blocking). Requires DB permission.
            try {
                Boolean got = jdbc.queryForObject("SELECT pg_try_advisory_lock(?)", Boolean.class, advisoryKey);
                advisoryLockAcquired = Boolean.TRUE.equals(got);
                if (!advisoryLockAcquired) {
                    log.warn("Recompute for org={} election={} could not acquire advisory lock; scheduling retry.", orgId, electionId);
                    // IMPORTANT: Throw a dedicated exception so caller/listener can schedule retry.
                    throw new AdvisoryLockNotAcquiredException("Another node holds advisory lock for org=" + orgId + " election=" + electionId);
                }
            } catch (AdvisoryLockNotAcquiredException ex) {
                throw ex; // rethrow so outer catch (if any) can handle scheduling
            } catch (Exception e) {
                // If advisory locks are not available or this query fails, log and continue with JVM guard only.
                log.warn("Could not run pg_try_advisory_lock (proceeding with JVM guard only): {}", e.getMessage());
                // do NOT throw here; we continue relying on JVM guard only
            }

            List<VoteSubmission> verifiedSubs =
                    voteSubmissionRepository.findByOrganization_OrgIdAndElection_ElectionIdAndStatus(
                            orgId, electionId, VoteStatus.VERIFIED);

            long verifiedCount = verifiedSubs.size();
            log.info("DEBUG: recomputeForElection org={} election={} -> {} VERIFIED submissions (JPA)",
                    orgId, electionId, verifiedCount);

            // 1) Aggregate per candidate using DB JSONB expansion.
            //    Use parameter for status so it's tied to VoteStatus enum.
            String sql = """
                SELECT (e.key)::uuid AS candidate_id,
                       SUM((e.value)::int) AS votes
                  FROM vote_submission s,
                       jsonb_each_text(s.candidate_votes) AS e(key, value)
                 WHERE s.org_id = ?
                   AND s.election_id = ?
                   AND s.status = ?
                 GROUP BY e.key
                """;

            // Pass the persisted representation of VERIFIED ("VERIFIED" with EnumType.STRING)
            List<Map<String, Object>> rows =
                    jdbc.queryForList(sql, orgId, electionId, VoteStatus.VERIFIED.name());

            log.info("DEBUG: recomputeForElection org={} election={} -> SQL produced {} candidate rows",
                    orgId, electionId, rows.size());

            // 2) Delete existing tallies for this org+election (atomic within transaction)
            voteTallyRepository.deleteByOrgAndElection(orgId, electionId);

            // 3) If no verified submissions / rows -> cleared tallies
            if (rows.isEmpty()) {
                log.info("No VERIFIED submissions (or no candidate_votes rows) for org={} election={}. Tallies cleared.",
                        orgId, electionId);
                return List.of();
            }

            // 4) Prepare candidate entities map
            List<UUID> candidateIds = rows.stream()
                    .map(r -> UUID.fromString(r.get("candidate_id").toString()))
                    .distinct()
                    .collect(Collectors.toList());

            Map<UUID, Candidate> candidateMap = candidateRepo.findAllById(candidateIds).stream()
                    .collect(Collectors.toMap(Candidate::getCandidateId, c -> c));

            // 5) Fetch full election entity (so DTO contains electionName etc.)
            Election election = electionRepo.findById(electionId).orElseGet(() -> {
                Election stub = new Election();
                stub.setElectionId(electionId);
                return stub;
            });

            // 6) Build new VoteTally entities
            LocalDateTime now = LocalDateTime.now();
            SystemUser recomputedBy = (recomputedByUserId == null) ? null :
                    systemUserRepo.findById(recomputedByUserId)
                            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Recomputed-by user not found"));

            List<VoteTally> toSave = new ArrayList<>(rows.size());
            for (Map<String, Object> r : rows) {
                UUID candidateId = UUID.fromString(r.get("candidate_id").toString());
                int totalVotes = ((Number) r.get("votes")).intValue();

                Candidate cand = candidateMap.get(candidateId);

                VoteTally v = new VoteTally();
                v.setOrganization(org);
                v.setElection(election);
                v.setCandidate(cand);
                v.setParty(cand != null ? cand.getParty() : null);
                v.setVoteCount(totalVotes);
                v.setLastRecomputedAt(now);
                v.setRecomputedBy(recomputedBy);

                toSave.add(v);
            }

            // 7) Persist new tallies (atomic within this transaction)
            List<VoteTally> saved = voteTallyRepository.saveAll(toSave);

            // 8) Map to DTOs and return
            List<VoteTallyDto> out = saved.stream().map(mapper::toDTO).collect(Collectors.toList());
            log.info("Recomputed {} tallies for org={} election={}", out.size(), orgId, electionId);
            return out;

        } finally {
            // release advisory lock if acquired
            if (advisoryLockAcquired) {
                try {
                    // use queryForObject for SELECT
                    Boolean unlocked = jdbc.queryForObject("SELECT pg_advisory_unlock(?)", Boolean.class, advisoryKey);
                    log.debug("pg_advisory_unlock returned {}", unlocked);
                } catch (Exception e) {
                    log.warn("Failed to release advisory lock for org={} election={}: {}", orgId, electionId, e.getMessage());
                }
            }
            // release JVM guard
            RUN_LOCKS.remove(lockKey);
        }
    }



    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private long computeAdvisoryKey(UUID orgId, UUID electionId) {
        // Create a stable 64-bit key from the two UUIDs.
        // We take a name-based UUID from the concatenated ids and combine MSB/LSB.
        UUID combined = UUID.nameUUIDFromBytes((orgId.toString() + "|" + electionId.toString()).getBytes(StandardCharsets.UTF_8));
        return combined.getMostSignificantBits() ^ combined.getLeastSignificantBits();
    }

}
