package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.ElectionPartyAssignRequest;
import Backend.ElectionVote.dto.ElectionPartyDto;
import Backend.ElectionVote.dto.ElectionPartyUpdateRequest;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.service.ElectionPartyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Manages which parties participate in a given election.
 *
 * Design:
 *  - Party list is GLOBAL, controlled centrally (NEC / platform admin).
 *  - election_party is also centrally managed: organizations (tenants) do NOT change it.
 *  - Tenants simply see/use this list when recording votes.
 */
@RestController
@RequestMapping("/api/elections/parties")
@RequiredArgsConstructor
public class ElectionPartyController {

    private final ElectionPartyService electionPartyService;
    private final AuthorizationService authz;

    /**
     * Add a party to an election.
     *
     * Security: Platform-level admin only (central NEC control).
     */
    @PostMapping("/{electionId}/parties")
    @ResponseStatus(HttpStatus.CREATED)
    public ElectionPartyDto addPartyToElection(
            @PathVariable UUID electionId,
            @Valid @RequestBody ElectionPartyAssignRequest req) {

        // Ensure body electionId matches path, or override from path
        req.setElectionId(electionId);
        // Platform-level auth
        authz.requirePlatformAdmin();

        return electionPartyService.addPartyToElection(req);
    }


    /**
     * List all parties participating in an election.
     *
     * Security: any authenticated tenant can read (no restriction here),
     *           but RLS + org_id will still apply on vote data.
     */
    @GetMapping
    public List<ElectionPartyDto> list(@PathVariable UUID electionId) {
        return electionPartyService.listPartiesForElection(electionId);
    }


    /**
     * Update ballot order / qualification for a party in an election.
     *
     * Security: Platform admin only.
     */
    @PutMapping("/{partyId}")
    public ElectionPartyDto update(
            @PathVariable UUID electionId,
            @PathVariable UUID partyId,
            @Valid @RequestBody ElectionPartyUpdateRequest req) {

        authz.requirePlatformAdmin();
        return electionPartyService.updateElectionParty(electionId, partyId, req);
    }

    /**
     * Remove a party from an election.
     *
     * Security: Platform admin only.
     */
    @DeleteMapping("/{partyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID electionId,
            @PathVariable UUID partyId) {

        authz.requirePlatformAdmin();
        electionPartyService.removePartyFromElection(electionId, partyId);
    }

}
