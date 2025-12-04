package Backend.ElectionVote.views.contro;

import Backend.ElectionVote.views.CenterStatsParty;
import Backend.ElectionVote.views.service.CenterStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Exposes a simple paginated endpoint for center-level stats.
 *
 * Route: GET /api/orgs/{orgId}/elections/{electionId}/centers
 *
 * Note:
 *  - This controller assumes authentication/authorization elsewhere.
 *  - The service uses SET LOCAL so the repository call runs under RLS scoped to the org.
 */
@RestController
@RequestMapping("/api/orgs/{orgId}/elections/{electionId}/centers")
public class CenterStatsController {

    private final CenterStatsService service;

    @Autowired
    public CenterStatsController(CenterStatsService service) {
        this.service = service;
    }

    @GetMapping
    public Page<CenterStatsParty> listCenterStats(
            @PathVariable("orgId") UUID orgId,
            @PathVariable("electionId") UUID electionId,
            Pageable pageable
    ) {
        return service.getCenterStats(orgId, electionId, pageable);
    }
}