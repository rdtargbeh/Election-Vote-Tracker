//package Backend.ElectionVote.views.contro;
//
//import Backend.ElectionVote.utility.TenantContext;
//import Backend.ElectionVote.views.CandidateCountyCompareView;
//import Backend.ElectionVote.views.repo.CandidateCompareRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/stats/compare")
//@RequiredArgsConstructor
//public class CompareStatsController {
//
//    private final CandidateCompareRepository candidateCompareRepository;
//
//
//    @GetMapping("/candidates/county")
//    @PreAuthorize("isAuthenticated()")
//    public List<CandidateCountyCompareView> getCandidateCountyCompare(
//            @RequestParam UUID electionId
//    ) {
//        UUID orgId = TenantContext.requireCurrentOrgId();
//
//        return candidateCompareRepository
//                .findCandidateCountyCompare(orgId, electionId);
//    }
//
//}