package Backend.ElectionVote.controller;


import Backend.ElectionVote.dto.*;
import Backend.ElectionVote.service.PollingCenterAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/allocations")
@RequiredArgsConstructor
public class PollingCenterAllocationController {

    private final PollingCenterAllocationService service;

    @PostMapping
    public PollingCenterAllocationDto create(@Valid @RequestBody PollingCenterAllocationCreateRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public PollingCenterAllocationDto update(@PathVariable UUID id, @RequestBody PollingCenterAllocationUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/{id}")
    public PollingCenterAllocationDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    public Page<PollingCenterAllocationDto> search(
            @RequestParam(required = false) UUID electionId,
            @RequestParam(required = false) UUID countyId,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID centerId,
            @PageableDefault(size = 20, sort = "pollingCenter.centerName") Pageable pageable
    ) {
        return service.search(electionId, countyId, districtId, centerId, pageable);
    }
}
