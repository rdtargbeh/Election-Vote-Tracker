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


    @GetMapping("/{id}")
    public ResponseEntity<PartyDto> get(@PathVariable UUID id) {
        Optional<PartyDto> dto = partyService.get(id);
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

    @PutMapping("/{id}")
    public PartyDto update(@PathVariable UUID id, @Valid @RequestBody PartyUpdateRequest req) {
        return partyService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        partyService.delete(id);
    }



}