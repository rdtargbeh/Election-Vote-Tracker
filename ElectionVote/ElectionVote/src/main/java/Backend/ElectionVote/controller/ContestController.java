package Backend.ElectionVote.controller;


import Backend.ElectionVote.dto.ContestCreateRequest;
import Backend.ElectionVote.dto.ContestDto;
import Backend.ElectionVote.dto.ContestUpdateRequest;
import Backend.ElectionVote.service.ContestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ContestDto> create(@Valid @RequestBody ContestCreateRequest req) {
        ContestDto created = contestService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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
    public ResponseEntity<ContestDto> update(
            @PathVariable UUID contestId,
            @Valid @RequestBody ContestUpdateRequest req
    ) {
        return ResponseEntity.ok(contestService.update(contestId, req));
    }


    @DeleteMapping("/{contestId}")
    public ResponseEntity<Void> delete(@PathVariable UUID contestId) {
        contestService.deleteContest(contestId);
        return ResponseEntity.noContent().build();
    }

}