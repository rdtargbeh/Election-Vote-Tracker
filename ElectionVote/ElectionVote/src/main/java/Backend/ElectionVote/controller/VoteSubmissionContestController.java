package Backend.ElectionVote.controller;


import Backend.ElectionVote.service.VoteSubmissionContestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints to trigger normalization of submissions into the normalized contest vote table.
 *
 * This API is NEC/ADMIN-only in production. Keep it protected via method security.
 */
@RestController
@RequestMapping("/api/admin/normalize")
@RequiredArgsConstructor
public class VoteSubmissionContestController {

    private final VoteSubmissionContestService normalizeService;

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<String> normalizeSubmission(@PathVariable UUID submissionId) {
        int created = normalizeService.normalizeSubmission(submissionId);
        return ResponseEntity.ok("Normalized rows created: " + created);
    }

    @PostMapping("/election/{electionId}/verified")
    public ResponseEntity<String> normalizeVerifiedForElection(@PathVariable UUID electionId) {
        int created = normalizeService.normalizeVerifiedSubmissionsForElection(electionId);
        return ResponseEntity.ok("Total normalized rows created for election: " + created);
    }
}
