package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.PartyCreateRequest;
import Backend.ElectionVote.dto.PartyDto;
import Backend.ElectionVote.dto.PartyUpdateRequest;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.service.PartyService;
import Backend.ElectionVote.utility.PartySearchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/parties")
@RequiredArgsConstructor
public class PartyController {

    private final AuthorizationService authz;
    private final PartyService partyService;


    @PostMapping
    public ResponseEntity<PartyDto> create(@Valid @RequestBody PartyCreateRequest req) {

        authz.requireNecAdminOrPlatformAdmin();
        PartyDto created = partyService.create(req);

        // Return result with HTTP 201
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @PutMapping("/{partyId}")
    public PartyDto update(@PathVariable UUID partyId, @Valid @RequestBody PartyUpdateRequest req) {
        authz.requireNecAdminOrPlatformAdmin();
        return partyService.update(partyId, req);
    }


    @GetMapping("/{partyId}")
    public ResponseEntity<PartyDto> get(@PathVariable UUID partyId) {
        Optional<PartyDto> dto = partyService.get(partyId);
        return dto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-abbrev/{abbr}")
    public ResponseEntity<PartyDto> getByAbbreviation(@PathVariable String abbr) {
        Optional<PartyDto> dto = partyService.getByAbbreviation(abbr);
        return dto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<PartyDto> search(@RequestParam(required = false) String q,
                                 @PageableDefault(size = 20, sort = "partyName") Pageable pageable) {
        return partyService.search(new PartySearchRequest(q), pageable);
    }


    @DeleteMapping("/{partyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID partyId) {
        authz.requireNecAdminOrPlatformAdmin();
        partyService.delete(partyId);
    }



}