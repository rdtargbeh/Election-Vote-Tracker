package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.*;
import Backend.ElectionVote.service.ObserverReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/observer-reports")
@RequiredArgsConstructor
public class ObserverReportController {

    private final ObserverReportService service;

    @PostMapping
    public ObserverReportDto create(@Valid @RequestBody ObserverReportCreateRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public ObserverReportDto update(@PathVariable UUID id,
                                    @RequestBody ObserverReportUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/{id}")
    public ObserverReportDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    public Page<ObserverReportDto> search(
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) UUID observerId,
            @RequestParam(required = false) UUID countyId,
            @RequestParam(required = false) UUID centerId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return service.search(orgId, observerId, countyId, centerId, type, resolved, from, to, q, pageable);
    }

    // Optional: proximity endpoint (returns a simple list)
    @GetMapping("/near")
    public java.util.List<ObserverReportDto> near(
            @RequestParam UUID orgId,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "2000") double meters
    ) {
        return service.near(orgId, lat, lon, meters);
    }
}