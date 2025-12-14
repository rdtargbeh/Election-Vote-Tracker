//package Backend.ElectionVote.controller;
//
//
//import Backend.ElectionVote.dto.NecResultPublicDto;
//import Backend.ElectionVote.service.NecResultPublishService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.UUID;
//
///**
// * Public-facing endpoints for published NEC results. These endpoints return data from
// * the public snapshot (nec_result_public / nec_result_geo view) and should be readonly.
// *
// * If you want to restrict access to authenticated parties (e.g. registered political parties),
// * apply method-level security or API gateway rules. By default these are public.
// */
//@RestController
//@RequestMapping("/api/public/results")
//@RequiredArgsConstructor
//public class NecResultPublicController {
//
//    private final NecResultPublishService publishService;
//
//    /**
//     * List published center results for an election.
//     * Returns a list of NecResultPublicDto (each entry represents a center's published result).
//     */
//    @GetMapping("/{electionId}/centers")
//    public List<NecResultPublicDto> listPublicByElection(@PathVariable UUID electionId) {
//        return publishService.listPublicResults(electionId);
//    }
//}