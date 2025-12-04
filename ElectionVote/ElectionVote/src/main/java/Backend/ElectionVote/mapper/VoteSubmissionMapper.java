package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.VoteSubmissionCreateRequest;
import Backend.ElectionVote.dto.VoteSubmissionDto;
import Backend.ElectionVote.dto.VoteSubmissionUpdateRequest;
import Backend.ElectionVote.entity.*;
import Backend.ElectionVote.enums.VoteStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;


@Component
public class VoteSubmissionMapper {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ───────────────────────────────────────────────────────────────
    //   ENTITY -> DTO
    //   Adds: structured candidateVotes, validVotes, invalidTotal
    // ───────────────────────────────────────────────────────────────
    public VoteSubmissionDto toDTO(VoteSubmission s) {
        Organization org = s.getOrganization();
        Election e = s.getElection();
        PollingPlace p = s.getPollingPlace();
        PollingCenter c = s.getPollingCenter();
        SystemUser a = s.getAgent();
        SystemUser v = s.getVerifiedBy();

        Double lat = null, lon = null;
        if (s.getGpsLocation() != null) {
            lon = s.getGpsLocation().getX();
            lat = s.getGpsLocation().getY();
        }

        String agentName = a != null ? a.getFirstName() + " " + a.getLastName() : null;
        String verifiedByName = v != null ? v.getFirstName() + " " + v.getLastName() : null;

        // candidateVotes now comes directly from the entity as a Map
        Map<UUID, Integer> votesMap =
                s.getCandidateVotes() != null ? s.getCandidateVotes() : Collections.emptyMap();

        int validVotes = votesMap.values().stream().mapToInt(Integer::intValue).sum();

        int invalidTotal =
                nz(s.getInvalidBallots()) +
                        nz(s.getBlankBallots()) +
                        nz(s.getRejectedBallots()) +
                        nz(s.getSpoiledBallots());

        // These are left null; your stats endpoint can compute them
        Double turnoutPct = null;
        Double invalidPct = null;

        // Serialize Map -> JSON string for the DTO field
        String candidateVotesJson = null;
        try {
            candidateVotesJson = objectMapper.writeValueAsString(votesMap);
        } catch (Exception ex) {
            candidateVotesJson = "{}"; // safe fallback
        }

        return VoteSubmissionDto.builder()
                .submissionId(s.getSubmissionId())

                .orgId(org.getOrgId())
                .orgName(org.getOrgName())

                .electionId(e.getElectionId())
                .electionName(e.getElectionName())
                .year(e.getYear())

                // Polling center reference
                .centerId(c.getCenterId())
                .centerCode(c.getCode())
                .centerName(c.getCenterName())

                // Polling place reference
                .placeId(p.getPlaceId())
                .placeCode(p.getCode())
                .placeNumber(p.getPlaceNumber())
                .placeLabel(p.getLabel())

                .agentId(a.getUserId())
                .agentName(agentName)

                .submissionTime(s.getSubmissionTime())

                .validVotes(validVotes)
                .invalidTotal(invalidTotal)
                .turnoutPct(turnoutPct)
                .invalidPct(invalidPct)

                // raw JSON for frontend (if needed)
                .candidateVotesJson(candidateVotesJson)
                // structured map for frontend
                .candidateVotes(votesMap)

                .ballotsCast(s.getBallotsCast())
                .invalidBallots(s.getInvalidBallots())
                .blankBallots(s.getBlankBallots())
                .rejectedBallots(s.getRejectedBallots())
                .spoiledBallots(s.getSpoiledBallots())
                .status(s.getStatus())
                .comments(s.getComments())

                .latitude(lat)
                .longitude(lon)

                .verifiedBy(v != null ? v.getUserId() : null)
                .verifiedByName(verifiedByName)
                .dateVerified(s.getDateVerified())

                .clientIp(s.getClientIp())
                .userAgent(s.getUserAgent())
                .submissionHash(s.getSubmissionHash())
                .version(s.getVersion())
                .idempotencyKey(s.getIdempotencyKey())

                .build();
    }

    // ───────────────────────────────────────────────────────────────
    //   CREATE: DTO -> Entity
    // ───────────────────────────────────────────────────────────────
    public VoteSubmission toEntity(
            VoteSubmissionCreateRequest req,
            Organization org, Election e, PollingCenter c, SystemUser agent) {

        VoteSubmission s = new VoteSubmission();

        s.setOrganization(org);
        s.setElection(e);
        s.setPollingCenter(c);
        s.setAgent(agent);

        // Store Map directly; Hibernate will handle JSONB
        s.setCandidateVotes(req.getCandidateVotes());

        s.setBallotsCast(nz(req.getBallotsCast()));
        s.setInvalidBallots(nz(req.getInvalidBallots()));
        s.setBlankBallots(nz(req.getBlankBallots()));
        s.setRejectedBallots(nz(req.getRejectedBallots()));
        s.setSpoiledBallots(nz(req.getSpoiledBallots()));

        s.setStatus(VoteStatus.PENDING);
        s.setComments(req.getComments());
        s.setClientIp(req.getClientIp());
        s.setUserAgent(req.getUserAgent());

        if (req.getLatitude() != null && req.getLongitude() != null) {
            s.setGpsLocation(point(req.getLongitude(), req.getLatitude()));
        }

        return s;
    }

    // ───────────────────────────────────────────────────────────────
    //   UPDATE: DTO -> existing Entity
    // ───────────────────────────────────────────────────────────────
    public void apply(VoteSubmissionUpdateRequest req, VoteSubmission s) {

        if (req.getCandidateVotes() != null)
            s.setCandidateVotes(req.getCandidateVotes());

        if (req.getBallotsCast() != null)
            s.setBallotsCast(req.getBallotsCast());

        if (req.getInvalidBallots() != null)
            s.setInvalidBallots(req.getInvalidBallots());

        if (req.getBlankBallots() != null)
            s.setBlankBallots(req.getBlankBallots());

        if (req.getRejectedBallots() != null)
            s.setRejectedBallots(req.getRejectedBallots());

        if (req.getSpoiledBallots() != null)
            s.setSpoiledBallots(req.getSpoiledBallots());

        if (req.getComments() != null)
            s.setComments(req.getComments());

        if (req.getLatitude() != null && req.getLongitude() != null)
            s.setGpsLocation(point(req.getLongitude(), req.getLatitude()));
    }

    // ───────────────────────────────────────────────────────────────
    //   Helpers
    // ───────────────────────────────────────────────────────────────
    private static Point point(double lon, double lat) {
        Point p = GF.createPoint(new Coordinate(lon, lat));
        p.setSRID(4326);
        return p;
    }

    private static int nz(Integer x) {
        return x == null ? 0 : x;
    }

}
