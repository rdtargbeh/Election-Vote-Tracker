package Backend.ElectionVote.controller;


import Backend.ElectionVote.dto.VoteSubmissionRankingDto;
import Backend.ElectionVote.service.VoteSubmissionRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin endpoints for submission vote ranking management (CRUD + backfill).
 * These endpoints are sensitive and should be protected (admin-only).
 */
@RestController
@RequestMapping("/api/admin/submission-rankings")
@RequiredArgsConstructor
public class VoteSubmissionRankingController {

    private final VoteSubmissionRankingService rankingService;

    @PostMapping
    public ResponseEntity<VoteSubmissionRankingDto> create(@RequestBody VoteSubmissionRankingDto dto) {
        VoteSubmissionRankingDto created = rankingService.createOrUpdateRanking(dto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{svrId}")
    public ResponseEntity<VoteSubmissionRankingDto> get(@PathVariable UUID svrId) {
        return ResponseEntity.ok(rankingService.getById(svrId));
    }

    @GetMapping("/by-submission/{submissionId}")
    public ResponseEntity<List<VoteSubmissionRankingDto>> bySubmission(@PathVariable UUID submissionId) {
        return ResponseEntity.ok(rankingService.getBySubmission(submissionId));
    }

    @GetMapping("/by-contest/{contestId}")
    public ResponseEntity<List<VoteSubmissionRankingDto>> byContest(@PathVariable UUID contestId) {
        return ResponseEntity.ok(rankingService.getByContest(contestId));
    }

    @PutMapping("/{svrId}")
    public ResponseEntity<VoteSubmissionRankingDto> update(@PathVariable UUID svrId, @RequestBody VoteSubmissionRankingDto dto) {
        dto.setSvrId(svrId);
        VoteSubmissionRankingDto updated = rankingService.createOrUpdateRanking(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{svrId}")
    public ResponseEntity<Void> delete(@PathVariable UUID svrId) {
        rankingService.deleteById(svrId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/election/{electionId}/backfill")
    public ResponseEntity<String> backfill(@PathVariable UUID electionId) {
        int created = rankingService.backfillFromVerifiedSubmissionsForElection(electionId);
        return ResponseEntity.ok("Backfilled ranking rows: " + created);
    }
}