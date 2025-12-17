package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.OrganizationCreateRequest;
import Backend.ElectionVote.dto.OrganizationDto;
import Backend.ElectionVote.dto.OrganizationUpdateRequest;
import Backend.ElectionVote.enums.OrganizationType;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.service.OrganizationService;
import Backend.ElectionVote.utility.OrganizationSearchRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/orgs")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private AuthorizationService authz;


    @PostMapping
    public ResponseEntity<OrganizationDto> create(@Valid @RequestBody OrganizationCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDto> get(@PathVariable UUID id) {
        return organizationService.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-subdomain/{sub}")
    public ResponseEntity<OrganizationDto> getBySubdomain(@PathVariable String sub) {
        Optional<OrganizationDto> dto = organizationService.getBySubdomain(sub);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<OrganizationDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) OrganizationType type,
            @PageableDefault(size = 20, sort = "dateCreated") Pageable pageable
    ) {
        OrganizationSearchRequest req = new OrganizationSearchRequest(q, active, type);
        return organizationService.search(req, pageable);
    }

    @PutMapping("/{id}")
    public OrganizationDto update(@PathVariable UUID id, @Valid @RequestBody OrganizationUpdateRequest req) {
        authz.requireNecAdminOrPlatformAdmin(); // Required System Admin or NEC Admin
        return organizationService.update(id, req);
    }

    @PatchMapping("/{id}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setActive(@PathVariable UUID id, @RequestBody @Valid SetActiveRequest body) {
        authz.requireNecAdminOrPlatformAdmin(); // Required System Admin or NEC Admin
        organizationService.setActive(id, body.active());
    }

    public record SetActiveRequest(@NotNull Boolean active) {}
    public record AssignPartyRequest(UUID partyId) {}
}