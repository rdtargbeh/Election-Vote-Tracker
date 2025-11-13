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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vote-submissions")
@RequiredArgsConstructor
public class VoteSubmissionController {

    private final VoteSubmissionService voteSubmissionService;


    // -------- CREATE (handles both with and without files) --------
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VoteSubmissionDto create(
            @RequestPart("payload") VoteSubmissionCreateRequest req,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        // if files are present, call the overloaded method
        return (files != null && !files.isEmpty())
                ? voteSubmissionService.create(req, files)
                : voteSubmissionService.create(req);
    }

    // -------- UPDATE (handles both with and without files) --------
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VoteSubmissionDto update(
            @PathVariable UUID id,
            @RequestPart("payload") VoteSubmissionUpdateRequest req,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return (files != null && !files.isEmpty())
                ? voteSubmissionService.update(id, req, files)
                : voteSubmissionService.update(id, req);
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


    //    @PostMapping
//    public VoteSubmissionDto create(@Valid @RequestBody VoteSubmissionCreateRequest req) {
//        return voteSubmissionService.create(req);
//    }
//
//    @PutMapping("/{id}")
//    public VoteSubmissionDto update(@PathVariable UUID id,
//                                    @RequestBody VoteSubmissionUpdateRequest req) {
//        return voteSubmissionService.update(id, req);
//    }
}
