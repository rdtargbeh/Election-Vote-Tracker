package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.ElectionCreateRequest;
import Backend.ElectionVote.dto.ElectionDto;
import Backend.ElectionVote.dto.ElectionSearchRequest;
import Backend.ElectionVote.dto.ElectionUpdateRequest;
import Backend.ElectionVote.enums.ElectionType;
import Backend.ElectionVote.service.ElectionService;
import Backend.ElectionVote.service.VoteSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/elections")
@RequiredArgsConstructor
public class ElectionController {

    @Autowired
    private ElectionService service;
    @Autowired
    private VoteSubmissionService voteSubmissionService;

    @PostMapping
    public ElectionDto create(@Valid @RequestBody ElectionCreateRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public ElectionDto update(@PathVariable UUID id, @Valid @RequestBody ElectionUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/{id}")
    public ElectionDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    public Page<ElectionDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) ElectionType type,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "year", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(ElectionSearchRequest.of(q, year, type, active), pageable);
    }





}
