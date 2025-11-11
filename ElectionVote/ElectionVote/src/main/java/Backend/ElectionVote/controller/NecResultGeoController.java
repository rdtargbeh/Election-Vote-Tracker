package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.NecResultGeoDto;
import Backend.ElectionVote.mapper.NecResultGeoMapper;
import Backend.ElectionVote.repository.NecResultGeoRepository;
import Backend.ElectionVote.utility.NecResultGeoSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/nec-results/geo")
@RequiredArgsConstructor
public class NecResultGeoController {

    private final NecResultGeoRepository repo;

    @GetMapping
    public Page<NecResultGeoDto> search(
            @RequestParam(required = false) UUID electionId,
            @RequestParam(required = false) UUID countyId,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID centerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadedAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadedBefore,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "uploadTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var spec = Specification
                .where(NecResultGeoSpecs.electionEquals(electionId))
                .and(NecResultGeoSpecs.countyEquals(countyId))
                .and(NecResultGeoSpecs.districtEquals(districtId))
                .and(NecResultGeoSpecs.centerEquals(centerId))
                .and(NecResultGeoSpecs.uploadedAfter(uploadedAfter))
                .and(NecResultGeoSpecs.uploadedBefore(uploadedBefore))
                .and(NecResultGeoSpecs.textSearch(q));

        return repo.findAll(spec, pageable).map(NecResultGeoMapper::toDTO);
    }

    @GetMapping("/{resultId}")
    public NecResultGeoDto get(@PathVariable UUID resultId) {
        return repo.findById(resultId)
                .map(NecResultGeoMapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Geo row not found"));
    }
}

