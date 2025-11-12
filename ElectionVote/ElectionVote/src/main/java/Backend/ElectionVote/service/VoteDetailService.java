package Backend.ElectionVote.service;


import Backend.ElectionVote.dto.VoteDetailCreateRequest;
import Backend.ElectionVote.dto.VoteDetailDto;
import Backend.ElectionVote.dto.VoteDetailUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface VoteDetailService {
    VoteDetailDto create(VoteDetailCreateRequest req);
    VoteDetailDto update(UUID detailId, VoteDetailUpdateRequest req);
    void delete(UUID detailId);
    VoteDetailDto get(UUID detailId);

    Page<VoteDetailDto> search(UUID orgId, UUID submissionId, UUID candidateId, UUID partyId, Pageable pageable);

    /** explode a submission's candidate_votes JSON into vote_detail rows (idempotent refresh) */
    List<VoteDetailDto> resyncFromSubmission(UUID submissionId);
}
