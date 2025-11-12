package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.VoteDetailCreateRequest;
import Backend.ElectionVote.dto.VoteDetailDto;
import Backend.ElectionVote.dto.VoteDetailUpdateRequest;
import Backend.ElectionVote.service.VoteDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vote-details")
@RequiredArgsConstructor
public class VoteDetailController {

    private final VoteDetailService service;

    @PostMapping
    public VoteDetailDto create(@Valid @RequestBody VoteDetailCreateRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public VoteDetailDto update(@PathVariable UUID id,
                                @RequestBody VoteDetailUpdateRequest req) {
        return service.update(id, req);
    }

    @GetMapping("/{id}")
    public VoteDetailDto get(@PathVariable UUID id) { return service.get(id); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) { service.delete(id); }

    @GetMapping
    public Page<VoteDetailDto> search(
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) UUID submissionId,
            @RequestParam(required = false) UUID candidateId,
            @RequestParam(required = false) UUID partyId,
            @PageableDefault(size = 20, sort = "detailId") Pageable pageable) {
        return service.search(orgId, submissionId, candidateId, partyId, pageable);
    }

    // Handy utility to rebuild from the JSON on demand (admin/tooling)
    @PostMapping("/resync/{submissionId}")
    public List<VoteDetailDto> resync(@PathVariable UUID submissionId) {
        return service.resyncFromSubmission(submissionId);
    }
}
