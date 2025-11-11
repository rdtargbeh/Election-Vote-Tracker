package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.VoteSubmissionCreateRequest;
import Backend.ElectionVote.dto.VoteSubmissionDto;
import Backend.ElectionVote.dto.VoteSubmissionUpdateRequest;
import Backend.ElectionVote.dto.VoteSubmissionVerifyRequest;
import Backend.ElectionVote.enums.VoteStatus;
import Backend.ElectionVote.service.VoteSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/vote-submissions")
@RequiredArgsConstructor
public class VoteSubmissionController {

    private final VoteSubmissionService voteSubmissionService;

    @PostMapping
    public VoteSubmissionDto create(@Valid @RequestBody VoteSubmissionCreateRequest req) {
        return voteSubmissionService.create(req);
    }

    @PutMapping("/{id}")
    public VoteSubmissionDto update(@PathVariable UUID id,
                                    @RequestBody VoteSubmissionUpdateRequest req) {
        return voteSubmissionService.update(id, req);
    }

    @PostMapping("/{id}/review")
    public VoteSubmissionDto review(@PathVariable UUID id,
                                    @Valid @RequestBody VoteSubmissionVerifyRequest req) {
        return voteSubmissionService.verify(id, req);
    }

    @GetMapping("/{id}")
    public VoteSubmissionDto get(@PathVariable UUID id) {
        return voteSubmissionService.get(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) { voteSubmissionService.delete(id); }

    @GetMapping
    public Page<VoteSubmissionDto> search(
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) UUID electionId,
            @RequestParam(required = false) UUID centerId,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) VoteStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "submissionTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return voteSubmissionService.search(orgId, electionId, centerId, agentId, status, from, to, q, pageable);
    }



    /**
     * Returns the total number of visible vote submissions.
     *
     * Example: GET /api/vote-submissions/count
     */
    @GetMapping("/count")
    public long countVisibleSubmissions() {
        return voteSubmissionService.countVisibleSubmissions();
    }
}
