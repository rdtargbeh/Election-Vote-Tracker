//package Backend.ElectionVote.views.contro;
//
//import Backend.ElectionVote.views.OfficialCandidateCountyStatsView;
//import Backend.ElectionVote.views.repo.OfficialStatsRepository;
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
//@RequestMapping("/api/stats/official")
//@RequiredArgsConstructor
//public class OfficialStatsController {
//
//    private final OfficialStatsRepository officialStatsRepository;
//
//    // Could be permitAll() if you want public exposure
//    @GetMapping("/candidates/county")
//    @PreAuthorize("isAuthenticated()") // or permitAll()
//    public List<OfficialCandidateCountyStatsView> getCandidateCountyStats(
//            @RequestParam UUID electionId,
//            @RequestParam(required = false) UUID countyId
//    ) {
//        if (countyId != null) {
//            return officialStatsRepository
//                    .findCandidateCountyStatsByElectionAndCounty(electionId, countyId);
//        }
//        return officialStatsRepository
//                .findCandidateCountyStatsByElection(electionId);
//    }
//}