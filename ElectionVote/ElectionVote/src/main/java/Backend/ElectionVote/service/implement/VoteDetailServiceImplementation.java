package Backend.ElectionVote.service.implement;


import Backend.ElectionVote.dto.VoteDetailCreateRequest;
import Backend.ElectionVote.dto.VoteDetailDto;
import Backend.ElectionVote.dto.VoteDetailUpdateRequest;
import Backend.ElectionVote.entity.*;
import Backend.ElectionVote.mapper.VoteDetailMapper;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.service.VoteDetailService;
import Backend.ElectionVote.utility.VoteDetailSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class VoteDetailServiceImplementation implements VoteDetailService {

    private final VoteDetailRepository repo;
    private final OrganizationRepository orgRepo;
    private final VoteSubmissionRepository submissionRepo;
    private final PartyRepository partyRepo;
    private final CandidateRepository candidateRepo;

    private final VoteDetailMapper mapper = new VoteDetailMapper();

    @Override
    @Transactional
    public VoteDetailDto create(VoteDetailCreateRequest req) {
        Organization org = orgRepo.findById(req.getOrgId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));
        VoteSubmission sub = submissionRepo.findById(req.getSubmissionId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));

        Party party = null;
        Candidate candidate = null;
        if (req.getCandidateId() != null) {
            candidate = candidateRepo.findById(req.getCandidateId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Candidate not found"));
            party = candidate.getParty(); // optional, may be null
        } else if (req.getPartyId() != null) {
            party = partyRepo.findById(req.getPartyId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Party not found"));
        }

        if (req.getVoteCount() < 0) throw new ResponseStatusException(BAD_REQUEST, "voteCount must be >= 0");

        VoteDetail saved = repo.save(mapper.toEntity(req, org, sub, party, candidate));
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public VoteDetailDto update(UUID detailId, VoteDetailUpdateRequest req) {
        VoteDetail v = repo.findById(detailId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Vote detail not found"));

        Party party = null;
        Candidate candidate = null;
        if (req.getCandidateId() != null) {
            candidate = candidateRepo.findById(req.getCandidateId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Candidate not found"));
            party = candidate.getParty();
        } else if (req.getPartyId() != null) {
            party = partyRepo.findById(req.getPartyId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Party not found"));
        }
        if (req.getVoteCount() != null && req.getVoteCount() < 0)
            throw new ResponseStatusException(BAD_REQUEST, "voteCount must be >= 0");

        mapper.apply(req, v, party, candidate);
        return mapper.toDTO(repo.save(v));
    }

    @Override
    @Transactional
    public void delete(UUID detailId) {
        if (!repo.existsById(detailId)) throw new ResponseStatusException(NOT_FOUND, "Vote detail not found");
        repo.deleteById(detailId);
    }

    @Override
    @Transactional(readOnly = true)
    public VoteDetailDto get(UUID detailId) {
        return repo.findById(detailId)
                .map(mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Vote detail not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VoteDetailDto> search(UUID orgId, UUID submissionId, UUID candidateId, UUID partyId, Pageable pageable) {
        Specification<VoteDetail> spec = Specification
                .where(VoteDetailSpecs.orgEquals(orgId))
                .and(VoteDetailSpecs.submissionEquals(submissionId))
                .and(VoteDetailSpecs.candidateEquals(candidateId))
                .and(VoteDetailSpecs.partyEquals(partyId));
        return repo.findAll(spec, pageable).map(mapper::toDTO);
    }

    @Override
    @Transactional
    public List<VoteDetailDto> resyncFromSubmission(UUID submissionId) {
        VoteSubmission sub = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found"));

        // parse JSON to Map<UUID,Integer>
        Map<UUID,Integer> votes = parseVotes(sub.getCandidateVotes());

        // replace all rows for this submission (simple & consistent)
        repo.deleteBySubmissionId(submissionId);

        Organization org = sub.getOrganization();
        List<VoteDetailDto> out = new ArrayList<>(votes.size());
        for (Map.Entry<UUID,Integer> e : votes.entrySet()) {
            UUID candidateId = e.getKey();
            int count = Math.max(0, e.getValue() == null ? 0 : e.getValue());

            Candidate cand = candidateRepo.findById(candidateId)
                    .orElse(null); // tolerate missing candidate ids; can also choose to throw

            VoteDetail v = new VoteDetail();
            v.setOrganization(org);
            v.setSubmission(sub);
            v.setCandidate(cand);
            v.setParty(cand != null ? cand.getParty() : null);
            v.setVoteCount(count);

            out.add(mapper.toDTO(repo.save(v)));
        }
        return out;
    }

    // ---------- helpers ----------
    private static Map<UUID,Integer> parseVotes(String json) {
        try {
            var type = new com.fasterxml.jackson.core.type.TypeReference<Map<UUID,Integer>>() {};
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, type);
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid candidate_votes JSON");
        }
    }
}
