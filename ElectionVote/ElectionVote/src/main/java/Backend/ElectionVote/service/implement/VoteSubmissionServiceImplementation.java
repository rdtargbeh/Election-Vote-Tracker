package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.VoteSubmissionCreateRequest;
import Backend.ElectionVote.dto.VoteSubmissionDto;
import Backend.ElectionVote.dto.VoteSubmissionUpdateRequest;
import Backend.ElectionVote.dto.VoteSubmissionVerifyRequest;
import Backend.ElectionVote.entity.*;
import Backend.ElectionVote.enums.VoteStatus;
import Backend.ElectionVote.mapper.VoteSubmissionMapper;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.service.VoteDetailService;
import Backend.ElectionVote.service.VoteSubmissionService;
import Backend.ElectionVote.utility.VoteSubmissionSpecs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;


@Service
@RequiredArgsConstructor
public class VoteSubmissionServiceImplementation implements VoteSubmissionService {

    private final EntityManager em;

    private final VoteSubmissionRepository voteSubmissionRepository;
    private final OrganizationRepository orgRepo;
    private final ElectionRepository electionRepo;
    private final PollingCenterRepository centerRepo;
    private final SystemUserRepository userRepo;
    private final VoteDetailRepository voteDetailRepository;
    private final VoteDetailService voteDetailService;
    private final PollingCenterAllocationRepository allocationRepo;


    private final VoteSubmissionMapper mapper = new VoteSubmissionMapper();

    @Override
    @Transactional
    public VoteSubmissionDto create(VoteSubmissionCreateRequest req) {
        Organization org = orgRepo.findById(req.getOrgId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));
        Election e = electionRepo.findById(req.getElectionId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Election not found"));
        PollingCenter c = centerRepo.findById(req.getCenterId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling center not found"));
        SystemUser agent = userRepo.findById(req.getAgentId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Agent not found"));

        // authoritative registered/issued from allocation
        var alloc = allocationRepo.findByElection_ElectionIdAndPollingCenter_CenterId(
                e.getElectionId(), c.getCenterId()
        ).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Center not allocated for this election"));

        // tally validation (same rules as NEC)
        validateTally(req.getCandidateVotes(),
                nz(req.getInvalidBallots()), nz(req.getBlankBallots()), nz(req.getRejectedBallots()), nz(req.getSpoiledBallots()),
                nz(req.getBallotsCast()), alloc.getRegisteredVoters(), alloc.getBallotsIssued());

        VoteSubmission s = mapper.toEntity(req, org, e, c, agent);
        s.setSubmissionHash(buildSubmissionHash(org.getOrgId(), e.getElectionId(), c.getCenterId(), agent.getUserId(),
                s.getCandidateVotes(), s.getBallotsCast(), s.getInvalidBallots(), s.getBlankBallots(),
                s.getRejectedBallots(), s.getSpoiledBallots()));

        if (voteSubmissionRepository.existsBySubmissionHash(s.getSubmissionHash())) {
            throw new ResponseStatusException(CONFLICT, "Duplicate submission (same content).");
        }

        s.setVersion(1);
        VoteSubmission saved = voteSubmissionRepository.save(s);
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public VoteSubmissionDto update(UUID id, VoteSubmissionUpdateRequest req) {
        VoteSubmission s = voteSubmissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));
        if (s.getStatus() != VoteStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Only PENDING submissions can be updated");
        }

        // merge values to validate
        Map<UUID,Integer> mergedVotes = req.getCandidateVotes() != null ? req.getCandidateVotes() : readVotes(s.getCandidateVotes());
        int cast    = req.getBallotsCast()    != null ? req.getBallotsCast()    : s.getBallotsCast();
        int invalid = req.getInvalidBallots() != null ? req.getInvalidBallots() : s.getInvalidBallots();
        int blank   = req.getBlankBallots()   != null ? req.getBlankBallots()   : s.getBlankBallots();
        int rej     = req.getRejectedBallots()!= null ? req.getRejectedBallots(): s.getRejectedBallots();
        int spo     = req.getSpoiledBallots() != null ? req.getSpoiledBallots() : s.getSpoiledBallots();

        var alloc = allocationRepo.findByElection_ElectionIdAndPollingCenter_CenterId(
                s.getElection().getElectionId(), s.getPollingCenter().getCenterId()
        ).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Center not allocated for this election"));

        validateTally(mergedVotes, invalid, blank, rej, spo, cast, alloc.getRegisteredVoters(), alloc.getBallotsIssued());

        mapper.apply(req, s);
        s.setVersion(s.getVersion() == null ? 1 : s.getVersion()+1);

        // recompute hash for idempotency across updates
        s.setSubmissionHash(buildSubmissionHash(
                s.getOrganization().getOrgId(), s.getElection().getElectionId(), s.getPollingCenter().getCenterId(),
                s.getAgent().getUserId(), s.getCandidateVotes(), s.getBallotsCast(), s.getInvalidBallots(),
                s.getBlankBallots(), s.getRejectedBallots(), s.getSpoiledBallots()
        ));

        if (voteSubmissionRepository.existsBySubmissionHash(s.getSubmissionHash())) {
            // If this exact content already exists on another submission, block
            // (optional: allow self-same by id compare)
        }

        return mapper.toDTO(voteSubmissionRepository.save(s));
    }

    @Override
    @Transactional
    public VoteSubmissionDto verify(UUID id, VoteSubmissionVerifyRequest req) {
        VoteSubmission s = voteSubmissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));

        SystemUser verifier = userRepo.findById(req.getVerifierUserId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Verifier not found"));

        if (s.getStatus() != VoteStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Submission already processed");
        }

        s.setStatus(req.getAccept() ? VoteStatus.VERIFIED : VoteStatus.REJECTED);
        s.setVerifiedBy(verifier);
        s.setDateVerified(LocalDateTime.now());

        // optional reviewer note appended to comments
        if (req.getComment() != null && !req.getComment().isBlank()) {
            String prefix = (s.getComments() == null ? "" : s.getComments() + "\n");
            s.setComments(prefix + "[review] " + req.getComment());
        }

        try {
            VoteSubmission saved = voteSubmissionRepository.save(s);

            if (saved.getStatus() == VoteStatus.VERIFIED) {
                // explode candidate_votes JSON into vote_detail rows
                voteDetailService.resyncFromSubmission(saved.getSubmissionId());
            } else {
                // ensure no details remain for non-verified
                voteDetailRepository.deleteBySubmissionId(saved.getSubmissionId());
            }

            return mapper.toDTO(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // e.g., uq_vs_verified_once (one VERIFIED per (org,election,center))
            throw new ResponseStatusException(CONFLICT,
                    "A verified submission already exists for this organization, election, and center");
        }
    }


    @Override
    @Transactional
    public void delete(UUID id) {
        if (!voteSubmissionRepository.existsById(id)) throw new ResponseStatusException(NOT_FOUND, "Submission not found");
        voteSubmissionRepository.deleteById(id); // or soft-delete if you need audit trail
    }

    @Override
    @Transactional(readOnly = true)
    public VoteSubmissionDto get(UUID id) {
        return voteSubmissionRepository.findById(id).map(mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VoteSubmissionDto> search(UUID orgId, UUID electionId, UUID centerId, UUID agentId,
                                          VoteStatus status, LocalDateTime from, LocalDateTime to, String q,
                                          Pageable pageable) {
        Specification<VoteSubmission> spec = Specification
                .where(VoteSubmissionSpecs.orgEquals(orgId))
                .and(VoteSubmissionSpecs.electionEquals(electionId))
                .and(VoteSubmissionSpecs.centerEquals(centerId))
                .and(VoteSubmissionSpecs.agentEquals(agentId))
                .and(VoteSubmissionSpecs.statusEquals(status))
                .and(VoteSubmissionSpecs.between(from, to))
                .and(VoteSubmissionSpecs.textSearch(q));

        return voteSubmissionRepository.findAll(spec, pageable).map(mapper::toDTO);
    }


    @Override
    @Transactional(readOnly = true)
    public long countVisibleSubmissions() {
        Object x = em.createNativeQuery("SELECT COUNT(*) FROM public.vote_submission").getSingleResult();
        return ((Number)x).longValue(); // RLS, if enabled, will scope this automatically
    }


    // ---------- helpers ----------

    private static void validateTally(Map<UUID,Integer> votes, int invalid, int blank, int rejected, int spoiled,
                                      int cast, int registered, Integer ballotsIssued) {
        long sumVotes = votes == null ? 0L : votes.values().stream().mapToLong(Integer::longValue).sum();
        validateTallyInternal(sumVotes, invalid, blank, rejected, spoiled, cast, registered, ballotsIssued);
    }

    private static void validateTallyInternal(long sumVotes, int invalid, int blank, int rejected, int spoiled,
                                              int cast, int registered, Integer ballotsIssued) {
        if (cast < 0 || registered < 0) throw new ResponseStatusException(BAD_REQUEST, "Negative counts not allowed");
        if (invalid < 0 || blank < 0 || rejected < 0 || spoiled < 0) throw new ResponseStatusException(BAD_REQUEST, "Negative category counts not allowed");
        long accounted = sumVotes + invalid + blank + rejected + spoiled;
        if (accounted > cast) throw new ResponseStatusException(BAD_REQUEST, "Accounted ballots exceed ballotsCast");
        if (ballotsIssued != null && cast > ballotsIssued) throw new ResponseStatusException(BAD_REQUEST, "ballotsCast exceeds ballotsIssued");
        if (cast > registered) throw new ResponseStatusException(BAD_REQUEST, "ballotsCast exceeds totalRegisteredVoters");
    }

    private static Map<UUID,Integer> readVotes(String json) {
        try {
            var type = new TypeReference<Map<UUID,Integer>>() {};
            return new ObjectMapper().readValue(json, type);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid candidateVotes JSON in stored submission");
        }
    }

    private static String buildSubmissionHash(UUID orgId, UUID electionId, UUID centerId, UUID agentId,
                                              String votesJson, int cast, int invalid, int blank, int rejected, int spoiled) {
        String payload = orgId + "|" + electionId + "|" + centerId + "|" + agentId + "|" +
                votesJson + "|" + cast + "|" + invalid + "|" + blank + "|" + rejected + "|" + spoiled;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static int nz(Integer x){ return x==null?0:x; }


}
