package Backend.ElectionVote.controller;


import Backend.ElectionVote.dto.ContestDto;
import Backend.ElectionVote.service.ContestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin endpoints for contest management.
 * These endpoints should be protected (NEC/admin-only) in production.
 */
@RestController
@RequestMapping("/api/admin/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    @PostMapping
    public ResponseEntity<ContestDto> create(@RequestBody ContestDto dto) {
        ContestDto created = contestService.createContest(dto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{contestId}")
    public ResponseEntity<ContestDto> get(@PathVariable UUID contestId) {
        return ResponseEntity.ok(contestService.getContest(contestId));
    }

    @GetMapping
    public ResponseEntity<List<ContestDto>> listByElection(@RequestParam UUID electionId) {
        return ResponseEntity.ok(contestService.listByElection(electionId));
    }

    @PutMapping("/{contestId}")
    public ResponseEntity<ContestDto> update(@PathVariable UUID contestId, @RequestBody ContestDto dto) {
        return ResponseEntity.ok(contestService.updateContest(contestId, dto));
    }

    @DeleteMapping("/{contestId}")
    public ResponseEntity<Void> delete(@PathVariable UUID contestId) {
        contestService.deleteContest(contestId);
        return ResponseEntity.noContent().build();
    }
}