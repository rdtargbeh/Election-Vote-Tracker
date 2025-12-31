package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.*;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.service.ElectionCandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/election-candidates")
@RequiredArgsConstructor
public class ElectionCandidateController {

    private final AuthorizationService authz;
    private final ElectionCandidateService electionCandidateService;


    @PostMapping
    public ElectionCandidateDto create(@Valid @RequestBody ElectionCandidateCreateRequest req) {
        authz.requireNecAdminOrPlatformAdmin();
        return electionCandidateService.create(req);
    }

    @PutMapping("/{id}")
    public ElectionCandidateDto update(@PathVariable UUID id, @RequestBody ElectionCandidateUpdateRequest req) {
        authz.requireNecAdminOrPlatformAdmin();
        return electionCandidateService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        authz.requireNecAdminOrPlatformAdmin();
        electionCandidateService.delete(id);
    }

    @GetMapping("/{electionId}/candidates")
    public List<ElectionCandidateDto> listByElection(
            @PathVariable UUID electionId
    ) {
        // If you want, you can add authz.requirePlatformAdmin() or tenant rules here.
        return electionCandidateService.listByElection(electionId);
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