package Backend.ElectionVote.service;


import Backend.ElectionVote.dto.VoteDetailCreateRequest;
import Backend.ElectionVote.dto.VoteDetailDto;
import Backend.ElectionVote.dto.VoteDetailUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * VoteDetailService
 *
 * Business logic:
 * - vote_detail is a *derived* table that normalizes candidate_votes JSON
 *   from vote_submission into per-candidate rows.
 * - It is generated automatically from VERIFIED submissions.
 * - It is not manually edited by agents; you should not expose create/update/delete
 *   operations in public controllers.
 */

public interface VoteDetailService {

    /**
     * Read-only search for diagnostics / admin / reporting.
     * Does NOT change data.
     */
    Page<VoteDetailDto> search(UUID orgId,
                                   UUID submissionId,
                                   UUID candidateId,
                                   UUID partyId,
                                   Pageable pageable);

    /**
     * Rebuild all vote_detail rows for a single submission from its candidate_votes map.
     *
     * Business rules:
     * - Only allowed for VERIFIED submissions; PENDING/REJECTED submissions
     *   do not produce vote_detail rows.
     * - Idempotent: it first deletes existing rows for this submission,
     *   then re-inserts from the current candidate_votes map.
     *
     * Used when:
     * - A submission is newly VERIFIED.
     * - An admin needs to re-sync after data fixes on the submission.
     */
    List<VoteDetailDto> resyncFromSubmission(UUID submissionId);


//    VoteDetailDto create(VoteDetailCreateRequest req);
//    VoteDetailDto update(UUID detailId, VoteDetailUpdateRequest req);
//    void delete(UUID detailId);
//    VoteDetailDto get(UUID detailId);


}
