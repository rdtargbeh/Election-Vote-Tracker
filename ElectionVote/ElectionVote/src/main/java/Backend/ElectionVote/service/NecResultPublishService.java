//package Backend.ElectionVote.service;
//
//import Backend.ElectionVote.dto.NecResultPublicDto;
//
//import java.util.List;
//import java.util.UUID;
//
//public interface NecResultPublishService {
//    /**
//     * Publish authoritative results (NECResult entries) for an election to nec_result_public.
//     * Returns number of published entries.
//     */
//    int publishResults(UUID electionId, UUID actorUserId);
//
//    /**
//     * Unpublish (remove) public results for an election.
//     */
//    void unpublishResults(UUID electionId, UUID actorUserId);
//
//    List<NecResultPublicDto> listPublicResults(UUID electionId);
//}
