package Backend.ElectionVote.controller;



import Backend.ElectionVote.dto.ContestOptionDto;
import Backend.ElectionVote.service.ContestOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin endpoints for contest options (CRUD).
 * Protect these endpoints with proper authorization in production (NEC/admin only).
 */
@RestController
@RequestMapping("/api/admin/contest-options")
@RequiredArgsConstructor
public class ContestOptionController {

    private final ContestOptionService optionService;

    @PostMapping
    public ResponseEntity<ContestOptionDto> create(@RequestBody ContestOptionDto dto) {
        ContestOptionDto created = optionService.createOption(dto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{optionId}")
    public ResponseEntity<ContestOptionDto> get(@PathVariable UUID optionId) {
        return ResponseEntity.ok(optionService.getOption(optionId));
    }

    @GetMapping
    public ResponseEntity<List<ContestOptionDto>> listByContest(@RequestParam UUID contestId,
                                                                @RequestParam(defaultValue = "true") boolean onlyActive) {
        List<ContestOptionDto> list = optionService.listByContest(contestId, onlyActive);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{optionId}")
    public ResponseEntity<ContestOptionDto> update(@PathVariable UUID optionId, @RequestBody ContestOptionDto dto) {
        ContestOptionDto updated = optionService.updateOption(optionId, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{optionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID optionId) {
        optionService.deleteOption(optionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-candidate/{candidateId}")
    public ResponseEntity<List<ContestOptionDto>> byCandidate(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(optionService.findByCandidateId(candidateId));
    }
}