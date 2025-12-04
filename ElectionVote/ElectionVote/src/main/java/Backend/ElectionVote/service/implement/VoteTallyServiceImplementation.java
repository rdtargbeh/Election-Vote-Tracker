package Backend.ElectionVote.service.implement;


import Backend.ElectionVote.dto.VoteTallyDto;
import Backend.ElectionVote.entity.*;

import Backend.ElectionVote.mapper.VoteTallyMapper;
import Backend.ElectionVote.repository.*;
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

@Service
@RequiredArgsConstructor
public class VoteTallyServiceImplementation implements VoteTallyService {


    private static final Logger log = LoggerFactory.getLogger(VoteTallyServiceImplementation.class);

    private final VoteTallyRepository repo;
    private final OrganizationRepository orgRepo;
    private final VoteSubmissionRepository submissionRepo;
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

        return repo.findAll(spec, pageable).map(mapper::toDTO);
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
                    log.warn("Recompute for org={} election={} skipped: another instance holds advisory lock.", orgId, electionId);
                    return List.of();
                }
            } catch (Exception e) {
                // If advisory locks are not available or this query fails, log and continue with JVM guard only.
                log.warn("Could not acquire advisory lock (proceeding with JVM guard only): {}", e.getMessage());
            }

            // 1) Aggregate per candidate using DB JSONB expansion.
            //    We guard value parsing by filtering to numeric strings only (^\d+$) to avoid cast errors.
            String sql = """
                    SELECT (e.key)::uuid   AS candidate_id,
                           SUM((e.value)::int) AS votes
                      FROM vote_submission s,
                           jsonb_each_text(s.candidate_votes) AS e(key, value)
                     WHERE s.org_id = ?
                       AND s.election_id = ?
                       AND s.status = 'VERIFIED'
                       AND e.value ~ '^\\\\d+$'
                     GROUP BY e.key
                    """;

            List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId, electionId);

            // 2) Delete existing tallies for this org+election (atomic within transaction)
            repo.deleteByOrgAndElection(orgId, electionId);

            // 3) If no verified submissions / rows -> cleared tallies
            if (rows.isEmpty()) {
                log.info("No VERIFIED submissions found for org={} election={}. Tallies cleared.", orgId, electionId);
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
            List<VoteTally> saved = repo.saveAll(toSave);

            // 8) Map to DTOs and return
            List<VoteTallyDto> out = saved.stream().map(mapper::toDTO).collect(Collectors.toList());
            log.info("Recomputed {} tallies for org={} election={}", out.size(), orgId, electionId);
            return out;

        } finally {
            // release advisory lock if acquired
            if (advisoryLockAcquired) {
                try {
                    jdbc.update("SELECT pg_advisory_unlock(?)", advisoryKey);
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



//    private final VoteTallyRepository repo;
//    private final OrganizationRepository orgRepo;
//    private final VoteSubmissionRepository submissionRepo;
//    private final PartyRepository partyRepo;
//    private final CandidateRepository candidateRepo;
//    private final SystemUserRepository systemUserRepo;
//
//    private final VoteTallyMapper mapper = new VoteTallyMapper();
//
//
//
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<VoteTallyDto> search(UUID orgId,
//                                     UUID electionId,
//                                     UUID candidateId,
//                                     UUID partyId,
//                                     Pageable pageable) {
//
//        Specification<VoteTally> spec = Specification
//                .where(VoteTallySpecs.orgEquals(orgId))
//                .and(VoteTallySpecs.electionEquals(electionId))
//                .and(VoteTallySpecs.candidateEquals(candidateId))
//                .and(VoteTallySpecs.partyEquals(partyId));
//
//        return repo.findAll(spec, pageable).map(mapper::toDTO);
//    }
//
//
//    /**
//     * Convenience overload – system recompute (no explicit user).
//     */
//    @Override
//    @Transactional
//    public List<VoteTallyDto> recomputeForElection(UUID orgId, UUID electionId) {
//        return recomputeForElection(orgId, electionId, null);
//    }
//
//    /**
//     * Recompute aggregated vote totals for all candidates in a given election
//     * using ONLY VERIFIED submissions, recording who triggered the recompute.
//     *
//     * @param orgId             tenant
//     * @param electionId        election
//     * @param recomputedByUserId optional user who triggered (null = system job)
//     */
//    @Override
//    @Transactional
//    public List<VoteTallyDto> recomputeForElection(UUID orgId,
//                                                    UUID electionId,
//                                                    UUID recomputedByUserId) {
//
//        Organization org = orgRepo.findById(orgId)
//                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));
//
//        // Fetch all VERIFIED submissions for this org + election
//        List<VoteSubmission> submissions =
//                submissionRepo.findByOrganization_OrgIdAndElection_ElectionIdAndStatus(
//                        orgId, electionId, VoteStatus.VERIFIED);
//
//        // Aggregate per candidateId
//        Map<UUID, Integer> aggregate = new HashMap<>();
//        Election election = null;
//
//        for (VoteSubmission sub : submissions) {
//            if (election == null) {
//                election = sub.getElection();
//            }
//
//            Map<UUID, Integer> votes = parseVotes(sub.getCandidateVotes());
//            for (Map.Entry<UUID, Integer> e : votes.entrySet()) {
//                UUID candidateId = e.getKey();
//                int count = Math.max(0, e.getValue() == null ? 0 : e.getValue());
//                aggregate.merge(candidateId, count, Integer::sum);
//            }
//        }
//
//        // Identify "who" recomputed
//        SystemUser recomputedBy = null;
//        if (recomputedByUserId != null) {
//            recomputedBy = systemUserRepo.findById(recomputedByUserId)
//                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Recomputed-by user not found"));
//        }
//
//        LocalDateTime now = LocalDateTime.now();
//
//        // Clear old totals for this org + election
//        repo.deleteByOrgAndElection(orgId, electionId);
//
//        // No VERIFIED submissions? Then we just cleared tallies and return empty list.
//        if (aggregate.isEmpty()) {
//            return List.of();
//        }
//
//        // Insert new totals
//        List<VoteTallyDto> out = new ArrayList<>(aggregate.size());
//
//        for (Map.Entry<UUID, Integer> e : aggregate.entrySet()) {
//            UUID candidateId = e.getKey();
//            int totalVotes = e.getValue();
//
//            Candidate cand = candidateRepo.findById(candidateId).orElse(null);
//
//            VoteTally v = new VoteTally();
//            v.setOrganization(org);
//            v.setElection(election);
//            v.setCandidate(cand);
//            v.setParty(cand != null ? cand.getParty() : null);
//            v.setVoteCount(totalVotes);
//
//            // NEW metadata
//            v.setLastRecomputedAt(now);
//            v.setRecomputedBy(recomputedBy);
//
//            VoteTally saved = repo.save(v);
//            out.add(mapper.toDTO(saved));
//        }
//
//        return out;
//    }
//
//    // ------------------------------------------------------------------------
//    // helpers
//    // ------------------------------------------------------------------------
//    private static Map<UUID, Integer> parseVotes(Map<UUID, Integer> votes) {
//        return (votes != null) ? votes : Collections.emptyMap();
//    }


}
