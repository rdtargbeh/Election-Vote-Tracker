package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.VoteSubmissionCreateRequest;
import Backend.ElectionVote.dto.VoteSubmissionDto;
import Backend.ElectionVote.dto.VoteSubmissionUpdateRequest;
import Backend.ElectionVote.dto.VoteSubmissionVerifyRequest;
import Backend.ElectionVote.enums.VoteStatus;
import Backend.ElectionVote.service.VoteSubmissionService;
import jakarta.servlet.http.HttpServletRequest;
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

    // JSON create (no files)
    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public VoteSubmissionDto createFromJson(@RequestBody VoteSubmissionCreateRequest req,
                                            HttpServletRequest request) {
        return voteSubmissionService.create(req, request);
    }

    // Multipart create with optional files
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public VoteSubmissionDto create(
            @RequestPart("payload") VoteSubmissionCreateRequest req,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            HttpServletRequest request
    ) {
        return (files != null && !files.isEmpty())
                ? voteSubmissionService.create(req, files, request)
                : voteSubmissionService.create(req, request);
    }

    // Update (multipart)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VoteSubmissionDto updateMultipart(
            @PathVariable UUID id,
            @RequestPart("payload") VoteSubmissionUpdateRequest req,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        // Call the 3-arg service method. When files are absent, pass null for the files parameter.
        return (files != null && !files.isEmpty())
                ? voteSubmissionService.update(id, req, files)
                : voteSubmissionService.update(id, req, null);
    }

    // Update (JSON)
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public VoteSubmissionDto updateJson(@PathVariable UUID id, @RequestBody VoteSubmissionUpdateRequest req) {
        // Service exposes an update method that accepts files; call with null for no files.
        return voteSubmissionService.update(id, req, null);
    }


    // Verify
    @PostMapping("/{id}/verify")
    public VoteSubmissionDto verify(@PathVariable UUID id, @Valid @RequestBody VoteSubmissionVerifyRequest req) {
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

    @GetMapping("/count")
    public long countVisibleSubmissions() {
        return voteSubmissionService.countVisibleSubmissions();
    }



//    private final VoteSubmissionService voteSubmissionService;
//
//
//
//    // ------------------ CREATE endpoints ------------------
//    /**
//     * JSON create endpoint for clients that don't need to send files.
//     * POST /api/vote-submissions
//     * Content-Type: application/json
//     */
//    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE,
//            produces = MediaType.APPLICATION_JSON_VALUE)
//    public VoteSubmissionDto createFromJson(
//            @RequestBody VoteSubmissionCreateRequest req,
//            HttpServletRequest request
//    ) {
//        // delegate to service passing the HttpServletRequest so server can capture clientIp/userAgent
//        return voteSubmissionService.create(req, request);
//    }
//
//
//
//    /**
//     * Multipart create endpoint for clients that may attach files.
//     * POST /api/vote-submissions
//     * Content-Type: multipart/form-data
//     *
//     * The payload part must be named "payload" and contain the JSON representation
//     * of VoteSubmissionCreateRequest. Files (if any) should be sent under the
//     * part name "files" (can be multiple).
//     */
//    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
//            produces = MediaType.APPLICATION_JSON_VALUE)
//    public VoteSubmissionDto create(
//            @RequestPart("payload") VoteSubmissionCreateRequest req,
//            @RequestPart(value = "files", required = false) List<MultipartFile> files,
//            HttpServletRequest request
//    ) {
//        // delegate to service passing the HttpServletRequest
//        return (files != null && !files.isEmpty())
//                ? voteSubmissionService.create(req, files, request)
//                : voteSubmissionService.create(req, request);
//    }
//
//
//    // ------------------ UPDATE / VERIFY / GET / DELETE / SEARCH ------------------
//
//    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public VoteSubmissionDto update(
//            @PathVariable UUID id,
//            @RequestPart("payload") VoteSubmissionUpdateRequest req,
//            @RequestPart(value = "files", required = false) List<MultipartFile> files
//    ) {
//        return (files != null && !files.isEmpty())
//                ? voteSubmissionService.update(id, req, files)
//                : voteSubmissionService.update(id, req);
//    }
//
//    @PostMapping("/{id}/verify")
//    public VoteSubmissionDto verify(@PathVariable UUID id,
//                                    @Valid @RequestBody VoteSubmissionVerifyRequest req) {
//        return voteSubmissionService.verify(id, req);
//    }
//
//
//    @GetMapping("/{id}")
//    public VoteSubmissionDto get(@PathVariable UUID id) {
//        return voteSubmissionService.get(id);
//    }
//
//    @DeleteMapping("/{id}")
//    public void delete(@PathVariable UUID id) { voteSubmissionService.delete(id); }
//
//    @GetMapping
//    public Page<VoteSubmissionDto> search(
//            @RequestParam(required = false) UUID orgId,
//            @RequestParam(required = false) UUID electionId,
//            @RequestParam(required = false) UUID centerId,
//            @RequestParam(required = false) UUID agentId,
//            @RequestParam(required = false) VoteStatus status,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
//            @RequestParam(required = false) String q,
//            @PageableDefault(size = 20, sort = "submissionTime", direction = Sort.Direction.DESC) Pageable pageable
//    ) {
//        return voteSubmissionService.search(orgId, electionId, centerId, agentId, status, from, to, q, pageable);
//    }
//
//    @GetMapping("/count")
//    public long countVisibleSubmissions() {
//        return voteSubmissionService.countVisibleSubmissions();
//    }


}
