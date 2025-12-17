package Backend.ElectionVote.controller;


import Backend.ElectionVote.dto.VoteTallyDto;
import Backend.ElectionVote.service.VoteTallyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


/**
 * REST API for aggregated vote tallies.
 *
 * Endpoints:
 *  - GET  /api/org/{orgId}/elections/{electionId}/vote-tallies
 *      -> paged search with optional candidateId / partyId filters
 *
 *  - POST /api/org/{orgId}/elections/{electionId}/vote-tallies/recompute
 *      -> recompute tallies for an election (optionally attributed to a user)
 */


@RestController
@RequestMapping("/api/elections/{electionId}/vote-tallies")
@RequiredArgsConstructor
public class VoteTallyController {

    private final VoteTallyService voteTallyService;

    /**
     * Search aggregated vote tallies for an org + election,
     * optionally filtered by candidateId and/or partyId.
     *
     * Example:
     *  GET /api/org/{orgId}/elections/{electionId}/vote-tallies?page=0&size=20
     *  GET /api/org/{orgId}/elections/{electionId}/vote-tallies?candidateId=...&partyId=...
     */
    @GetMapping
    public Page<VoteTallyDto> search(@PathVariable("orgId") UUID orgId,
                                     @PathVariable("electionId") UUID electionId,
                                     @RequestParam(value = "candidateId", required = false) UUID candidateId,
                                     @RequestParam(value = "partyId", required = false) UUID partyId,
                                     @PageableDefault(size = 20) Pageable pageable) {
        return voteTallyService.search(orgId, electionId, candidateId, partyId, pageable);
    }


    /**
     * Recompute tallies for an election using VERIFIED submissions only.
     *
     * If recomputedByUserId is provided, it will be recorded;
     * otherwise the service treats it as a system-triggered recompute.
     *
     * Example:
     *  POST /api/org/{orgId}/elections/{electionId}/vote-tallies/recompute
     *  POST /api/org/{orgId}/elections/{electionId}/vote-tallies/recompute?recomputedByUserId=...
     */
    @PostMapping("/recompute")
    public List<VoteTallyDto> recompute(@PathVariable("orgId") UUID orgId,
                                        @PathVariable("electionId") UUID electionId,
                                        @RequestParam(value = "recomputedByUserId", required = false) UUID recomputedByUserId) {

        if (recomputedByUserId == null) {
            // convenience overload – system recompute (no explicit user)
            return voteTallyService.recomputeForElection(orgId, electionId);
        } else {
            return voteTallyService.recomputeForElection(orgId, electionId, recomputedByUserId);
        }
    }



//    @GetMapping
//    public Page<VoteTallyDto> search(
//            @RequestParam(required = false) UUID orgId,
//            @RequestParam(required = false) UUID submissionId,
//            @RequestParam(required = false) UUID candidateId,
//            @RequestParam(required = false) UUID partyId,
//            @PageableDefault(size = 20, sort = "detailId") Pageable pageable) {
//        return voteTallyService.search(orgId, submissionId, candidateId, partyId, pageable);
//    }
//
//    // Handy utility to rebuild from the JSON on demand (admin/tooling)
//    @PostMapping("/resync/{submissionId}")
//    public List<VoteTallyDto> resync(@PathVariable UUID submissionId) {
//        return voteTallyService.recomputeForElection(submissionId);
//    }


}
