package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.*;
import Backend.ElectionVote.service.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService service;

    @PostMapping
    public CandidateDto create(@Valid @RequestBody CandidateCreateRequest req) {
        return service.create(req);
    }

    @PutMapping("/{candidateId}")
    public CandidateDto update(@PathVariable UUID candidateId, @RequestBody CandidateUpdateRequest req) {
        return service.update(candidateId, req);
    }

    @DeleteMapping("/{candidateId}")
    public void delete(@PathVariable UUID candidateId) {
        service.delete(candidateId);
    }

    @GetMapping("/{candidateId}")
    public CandidateDto get(@PathVariable UUID candidateId) {
        return service.get(candidateId);
    }

    @GetMapping
    public Page<CandidateDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) UUID partyId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        return service.search(q, position, partyId, active, pageable);
    }
}
