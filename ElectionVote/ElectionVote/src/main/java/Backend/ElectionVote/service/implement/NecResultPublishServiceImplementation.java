//package Backend.ElectionVote.service.implement;
//
//import Backend.ElectionVote.dto.NecResultPublicDto;
//import Backend.ElectionVote.entity.NECResult;
//import Backend.ElectionVote.entity.NecResultStaging;
//import Backend.ElectionVote.mapper.NECResultMapper;
//import Backend.ElectionVote.repository.NECResultRepository;
//import Backend.ElectionVote.repository.NecResultPublicRepository;
//import Backend.ElectionVote.repository.NecResultStagingRepository;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import Backend.ElectionVote.repository.SystemUserRepository;
//import Backend.ElectionVote.service.AuditLogService;
//import Backend.ElectionVote.service.NecResultPublishService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
///**
// * Publish validated staging rows into the public snapshot (nec_result_public).
// * - Prefers authoritative NECResult data when a staging row has been promoted (processedResultId set).
// * - Uses JDBC to write JSONB safely and to avoid JPA composite-key complications for inserts.
// * - Marks staging rows as published and audited.
// *
// * IMPORTANT: secure publish/unpublish endpoints to NEC-only in production.
// */
//
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class NecResultPublishServiceImplementation implements NecResultPublishService {
//
//    private final NecResultStagingRepository stagingRepo;
//    private final SystemUserRepository userRepo;
//    private final NecResultPublicRepository publicRepo;
//    private final NECResultRepository necResultRepo;
//    private final AuditLogService auditLogService;
//    private final NECResultMapper mapper = new NECResultMapper();
//    private final JdbcTemplate jdbc;
//
//    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
//
//
//    @Override
//    public int publishResults(UUID electionId, UUID actorUserId) {
//        // Find validated, unpublished staging rows for election
//        List<NecResultStaging> rows = stagingRepo.findByElectionIdAndValidatedTrueAndIsPublishedFalse(electionId);
//        if (rows.isEmpty()) return 0;
//
//        // Delete existing public results for the election (atomicity ensured by transaction)
//        jdbc.update("DELETE FROM nec_result_public WHERE election_id = ?", electionId);
//
//        int published = 0;
//        LocalDateTime now = LocalDateTime.now();
//        for (NecResultStaging s : rows) {
//            // ensure assignedCenterId exists and candidate_votes present
//            UUID centerId = s.getAssignedCenterId();
//            if (centerId == null) continue;
//
//            // Prefer authoritative NECResult if available (promoted from staging). Otherwise fall back to staging values.
//            Map<String, Integer> votesMap = s.getCandidateVotes();
//            Integer totalRegistered = s.getTotalRegisteredVoters();
//            Integer ballotsCast = s.getBallotsCast();
//            Integer invalid = s.getInvalidBallots();
//            Integer blank = s.getBlankBallots();
//            Integer rejected = s.getRejectedBallots();
//            Integer spoiled = s.getSpoiledBallots();
//            String centerCode = s.getCenterCode();
//            String source = s.getSource();
//
//            if (s.getProcessedResultId() != null) {
//                Optional<NECResult> authOpt = necResultRepo.findById(s.getProcessedResultId());
//                if (authOpt.isPresent()) {
//                    NECResult auth = authOpt.get();
//                    // candidate votes parsing helper
//                    votesMap = NECResultMapper.parseToMapString(auth.getCandidateVotes());
//                    totalRegistered = auth.getTotalRegisteredVoters();
//                    ballotsCast = auth.getBallotsCast();
//                    invalid = auth.getInvalidBallots();
//                    blank = auth.getBlankBallots();
//                    rejected = auth.getRejectedBallots();
//                    spoiled = auth.getSpoiledBallots();
//                    if (auth.getPollingCenter() != null && (centerCode == null || centerCode.isBlank())) {
//                        // use the getter name that matches your PollingCenter entity
//                        centerCode = auth.getPollingCenter().getCode();
//                    }
//                    if (auth.getSource() != null) source = auth.getSource();
//                }
//            }
//
//            // Insert into nec_result_public (use JDBC for JSONB handling)
//            String insertSql = "INSERT INTO nec_result_public (election_id, assigned_center_id, center_code, candidate_votes, total_registered_voters, ballots_cast, invalid_ballots, blank_ballots, rejected_ballots, spoiled_ballots, source, published_at) " +
//                    "VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?)";
//            jdbc.update(insertSql,
//                    s.getElectionId(),
//                    centerId,
//                    centerCode,
//                    votesMap == null ? "{}" : toJsonSafe(votesMap),
//                    totalRegistered,
//                    ballotsCast,
//                    invalid,
//                    blank,
//                    rejected,
//                    spoiled,
//                    source,
//                    now
//            );
//
//            // mark staging as published
//            s.setIsPublished(true);
//            if (actorUserId != null) {
//                userRepo.findById(actorUserId).ifPresent(s::setPublishedBy);
//            }
//            s.setPublishedAt(now);
//            s.setProcessed(true);
//            s.setProcessedAt(now);
//            stagingRepo.save(s);
//            published++;
//        }
//
//        try {
//            auditLogService.logCreate(null, actorUserId, "NecResultPublic",
//                    "Published results for election=" + electionId + " rows=" + published);
//        } catch (Exception ex) {
//            // best-effort
//        }
//
//        return published;
//    }
//
//
//
//    @Override
//    public void unpublishResults(UUID electionId, UUID actorUserId) {
//        int rows = jdbc.update("DELETE FROM nec_result_public WHERE election_id = ?", electionId);
//
//        // mark staging rows is_published = false for the election (optional)
//        jdbc.update("UPDATE nec_result_staging SET is_published = FALSE WHERE election_id = ?", electionId);
//
//        try {
//            auditLogService.logDelete(null, actorUserId, "NecResultPublic",
//                    "Unpublished results for election=" + electionId + " rows_removed=" + rows);
//        } catch (Exception ex) {
//            // best-effort
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<NecResultPublicDto> listPublicResults(UUID electionId) {
//        return publicRepo.findByElectionId(electionId).stream().map(mapper::toPublicDto).collect(Collectors.toList());
//    }
//
//
//    private String toJsonSafe(Object value) {
//        try {
//            return OBJECT_MAPPER.writeValueAsString(value);
//        } catch (JsonProcessingException e) {
//            throw new IllegalStateException("Failed to serialize JSON", e);
//        }
//    }
//
//
//}