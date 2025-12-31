package Backend.ElectionVote.service;



import java.util.UUID;

/**
 * Service to normalize submissions into submission_contest_vote rows.
 */
public interface VoteSubmissionContestService {
    /**
     * Normalize a single submission into submission_contest_vote rows.
     * This is idempotent: it deletes any existing normalized rows for the submission before inserting.
     *
     * @param submissionId the submission to normalize
     * @return number of normalized rows created
     */
    int normalizeSubmission(UUID submissionId);

    /**
     * Normalize all VERIFIED submissions for an election (useful for backfill).
     * Returns total rows normalized (sum over submissions).
     */
    int normalizeVerifiedSubmissionsForElection(UUID electionId);
}