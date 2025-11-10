package Backend.ElectionVote.controller;

import Backend.ElectionVote.service.VoteSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vote-submissions")
@RequiredArgsConstructor
public class VoteSubmissionController {

    @Autowired
    private VoteSubmissionService voteSubmissionService;


    /**
     * Returns the total number of visible vote submissions.
     *
     * Example: GET /api/vote-submissions/count
     */
    @GetMapping("/count")
    public long countVisibleSubmissions() {
        return voteSubmissionService.countVisibleSubmissions();
    }
}
