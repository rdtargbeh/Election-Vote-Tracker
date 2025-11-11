package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.*;
import Backend.ElectionVote.service.ElectionCandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/election-candidates")
@RequiredArgsConstructor
public class ElectionCandidateController {

    @Autowired
    private ElectionCandidateService electionCandidateService;

    @PostMapping
    public ElectionCandidateDto create(@Valid @RequestBody ElectionCandidateCreateRequest req) {
        return electionCandidateService.create(req);
    }

    @PutMapping("/{id}")
    public ElectionCandidateDto update(@PathVariable UUID id, @RequestBody ElectionCandidateUpdateRequest req) {
        return electionCandidateService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        electionCandidateService.delete(id);
    }

    @GetMapping("/{id}")
    public ElectionCandidateDto get(@PathVariable UUID id) {
        return electionCandidateService.get(id);
    }

    @GetMapping
    public Page<ElectionCandidateDto> getAll(@PageableDefault(size = 20, sort = "electId") Pageable pageable) {
        return electionCandidateService.getAll(pageable);
    }
}