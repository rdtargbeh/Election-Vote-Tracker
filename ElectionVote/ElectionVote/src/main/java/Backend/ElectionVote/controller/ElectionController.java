package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.ElectionCreateRequest;
import Backend.ElectionVote.dto.ElectionDto;
import Backend.ElectionVote.dto.ElectionSearchRequest;
import Backend.ElectionVote.dto.ElectionUpdateRequest;
import Backend.ElectionVote.enums.ElectionType;
import Backend.ElectionVote.security.AuthorizationService;
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
    private final AuthorizationService authz;

    /**
     * Create a new election.
     * Only platform admins (SYSTEM_ADMIN / NEC-level) may create elections.
     */
    @PostMapping
    public ElectionDto create(@Valid @RequestBody ElectionCreateRequest req) {
        authz.requirePlatformAdmin();   // <-- protect creation
        return service.create(req);
    }

    /**
     * Update an existing election.
     * Only platform admins may update elections.
     */
    @PutMapping("/{id}")
    public ElectionDto update(@PathVariable UUID id,
                              @Valid @RequestBody ElectionUpdateRequest req) {
        authz.requirePlatformAdmin();   // <-- protect update
        return service.update(id, req);
    }

    /**
     * Delete an election by id.
     * Only platform admins may delete elections.
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        authz.requirePlatformAdmin();   // <-- protect delete
        service.delete(id);
    }

    /**
     * Retrieve a single election by id.
     * Read-only; no special authorization (tenants can see which elections exist).
     */
    @GetMapping("/{id}")
    public ElectionDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    /**
     * Search elections with optional filters (q, year, type, active).
     * Read-only; open to authenticated callers.
     */
    @GetMapping
    public Page<ElectionDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) ElectionType type,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "year", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return service.search(ElectionSearchRequest.of(q, year, type, active), pageable);
    }


}
