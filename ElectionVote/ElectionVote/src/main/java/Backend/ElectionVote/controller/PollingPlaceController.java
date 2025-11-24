package Backend.ElectionVote.controller;


import Backend.ElectionVote.dto.PollingPlaceCreateRequest;
import Backend.ElectionVote.dto.PollingPlaceDto;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.service.PollingPlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/polling-places")
@RequiredArgsConstructor
public class PollingPlaceController {

    private final AuthorizationService authz;
    private final PollingPlaceService pollingPlaceService;

    /**
     * Create a new polling place under a given center.
     *
     * - placeNumber is auto-generated (1,2,3,...) per center
     * - place code is auto-generated (PP-DDD-CCC-RAND5)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PollingPlaceDto create(@Valid @RequestBody PollingPlaceCreateRequest req) {
//        authz.requirePlatformAdmin();
        return pollingPlaceService.create(req);
    }

    /**
     * Get a single polling place by its ID.
     */
    @GetMapping("/{id}")
    public PollingPlaceDto get(@PathVariable("id") UUID id) {
        return pollingPlaceService.get(id);
    }

    /**
     * List all polling places for a specific center, ordered by placeNumber.
     *
     * Example:
     *   GET /api/polling-places/by-center/{centerId}
     */
    @GetMapping("/by-center/{centerId}")
    public List<PollingPlaceDto> listByCenter(@PathVariable("centerId") UUID centerId) {
        return pollingPlaceService.listByCenter(centerId);
    }

    /**
     * Deactivate a polling place (soft delete).
     *
     * This sets is_active = false but keeps the row and code for history.
     * If you expose hard delete, you can add a separate DELETE endpoint.
     */
    @DeleteMapping("/{id}")
    public PollingPlaceDto deactivate(@PathVariable("id") UUID id) {
        return pollingPlaceService.deactivate(id);
    }
}
