package Backend.ElectionVote.controller;


import Backend.ElectionVote.dto.PollingPlaceAllocationCreateRequest;
import Backend.ElectionVote.dto.PollingPlaceAllocationDto;
import Backend.ElectionVote.dto.PollingPlaceAllocationUpdateRequest;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.service.PollingPlaceAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/polling-place-allocations")
@RequiredArgsConstructor
public class PollingPlaceAllocationController {

    private final AuthorizationService authz;
    private final PollingPlaceAllocationService allocationService;

    /**
     * Create a new allocation for a polling place in a given election.
     *
     * Constraints enforced:
     *  - One allocation per (election, place)
     *  - ballotsIssued <= registeredVoters
     *  - non-negative counts
     */
    @PostMapping
    public PollingPlaceAllocationDto create(
            @Valid @RequestBody PollingPlaceAllocationCreateRequest req) {
        authz.requirePlatformAdmin();
        return allocationService.create(req);
    }

    /**
     * Get a single allocation by its ID.
     */
    @GetMapping("/{id}")
    public PollingPlaceAllocationDto get(@PathVariable("id") UUID id) {
        return allocationService.get(id);
    }

    /**
     * Update an existing allocation.
     *
     * Supports partial updates for:
     *  - registeredVoters
     *  - ballotsIssued
     */
    @PutMapping("/{id}")
    public PollingPlaceAllocationDto update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody PollingPlaceAllocationUpdateRequest req) {
        return allocationService.update(id, req);
    }

    /**
     * Delete an allocation.
     *
     * NOTE: You may want to protect this if results already exist for that place/election.
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") UUID id) {
        allocationService.delete(id);
    }

    /**
     * Search allocations with optional filters:
     *
     *  - electionId (UUID)
     *  - centerId   (UUID)
     *  - placeId    (UUID)
     *
     * Example:
     *   GET /api/polling-place-allocations?electionId=...&centerId=...&page=0&size=20
     */
    @GetMapping
    public Page<PollingPlaceAllocationDto> search(
            @RequestParam(value = "electionId", required = false) UUID electionId,
            @RequestParam(value = "centerId",   required = false) UUID centerId,
            @RequestParam(value = "placeId",    required = false) UUID placeId,
            Pageable pageable) {

        return allocationService.search(electionId, centerId, placeId, pageable);
    }
}
