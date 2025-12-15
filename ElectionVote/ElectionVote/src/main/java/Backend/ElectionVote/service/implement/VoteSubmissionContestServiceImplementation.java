package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.entity.ContestOption;
import Backend.ElectionVote.entity.VoteSubmissionContest;
import Backend.ElectionVote.entity.VoteSubmission;
import Backend.ElectionVote.repository.ContestOptionRepository;
import Backend.ElectionVote.repository.VoteSubmissionContestRepository;
import Backend.ElectionVote.repository.VoteSubmissionRepository;
import Backend.ElectionVote.service.VoteSubmissionContestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Normalizes VoteSubmission.candidate_votes -> submission_contest_vote (normalized table).
 *
 * Behavior and decisions:
 * - Idempotent: remove prior submission_contest_vote rows for the submission before inserting updated ones.
 * - Mapping: For each candidateId key in candidate_votes, attempts to resolve a ContestOption by candidateId.
 *   If multiple options exist, the first match is used. If none found, that candidate's votes are skipped,
 *   and a warning is logged (but the process continues).
 * - Vote values are inserted as-is (must be >= 0). Rank is null (unless ranking information is available elsewhere).
 */
@Service
@RequiredArgsConstructor
public class VoteSubmissionContestServiceImplementation implements VoteSubmissionContestService {

    private final VoteSubmissionContestRepository scvRepo;
    private final VoteSubmissionRepository vsRepo;
    private final ContestOptionRepository optionRepo;

    @Override
    @Transactional
    public int normalizeSubmission(UUID submissionId) {
        VoteSubmission vs = vsRepo.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found: " + submissionId));

        // parse candidate votes - assume VoteSubmission.getCandidateVotes() returns Map<String,Integer>
        Map<String, Integer> candidateVotes = vs.getCandidateVotes();
        if (candidateVotes == null || candidateVotes.isEmpty()) return 0;

        // delete existing normalized rows for idempotency
        scvRepo.deleteBySubmissionId(submissionId);

        int created = 0;
        UUID orgId = vs.getOrganization() != null ? vs.getOrganization().getOrgId() : null;
        UUID electionId = vs.getElection() != null ? vs.getElection().getElectionId() : null;

        for (Map.Entry<String, Integer> e : candidateVotes.entrySet()) {
            String candidateKey = e.getKey();
            Integer votes = e.getValue();
            if (candidateKey == null || candidateKey.isBlank() || votes == null) continue;
            UUID candidateId;
            try {
                candidateId = UUID.fromString(candidateKey);
            } catch (IllegalArgumentException ex) {
                // skip invalid candidate id
                continue;
            }

            List<ContestOption> options = optionRepo.findByCandidateId(candidateId);
            if (options == null || options.isEmpty()) {
                // No mapping found - skip; operator may need to fix contest_option table
                continue;
            }

            // choose first matching option (if predicate on election needed, extend repository/logic)
            ContestOption opt = options.get(0);

            VoteSubmissionContest scv = new VoteSubmissionContest();
            scv.setSubmissionId(submissionId);
            scv.setOrgId(orgId);
            scv.setElectionId(electionId);
            scv.setContestId(opt.getContestId());
            scv.setOptionId(opt.getOptionId());
            scv.setVoteValue(Math.max(0, votes));
            scv.setRank(null);

            scvRepo.save(scv);
            created++;
        }

        return created;
    }

    @Override
    @Transactional
    public int normalizeVerifiedSubmissionsForElection(UUID electionId) {
        List<VoteSubmission> subs = vsRepo.findByElection_ElectionIdAndStatusAndDateDeletedIsNull(electionId, "VERIFIED");
        int total = 0;
        for (VoteSubmission vs : subs) {
            total += normalizeSubmission(vs.getSubmissionId());
        }
        return total;
    }
}