package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.DistrictDto;
import Backend.ElectionVote.dto.DistrictRequest;
import Backend.ElectionVote.service.DistrictService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
public class DistrictController {

    @Autowired
    private DistrictService service;

    @PostMapping
    public ResponseEntity<DistrictDto> create(@Valid @RequestBody DistrictRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DistrictDto> update(@PathVariable UUID id,
                                              @Valid @RequestBody DistrictRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistrictDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<DistrictDto>> list(@RequestParam(value = "q", required = false) String q,
                                                  @RequestParam(value = "countyId", required = false) UUID countyId,
                                                  Pageable pageable) {
        return ResponseEntity.ok(service.list(q, countyId, pageable));
    }
}
