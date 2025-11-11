package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.VoteSubmissionCreateRequest;
import Backend.ElectionVote.dto.VoteSubmissionDto;
import Backend.ElectionVote.dto.VoteSubmissionUpdateRequest;
import Backend.ElectionVote.dto.VoteSubmissionVerifyRequest;
import Backend.ElectionVote.enums.VoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VoteSubmissionService {
    long countVisibleSubmissions();

    VoteSubmissionDto create(VoteSubmissionCreateRequest req);
    VoteSubmissionDto update(UUID submissionId, VoteSubmissionUpdateRequest req);
    VoteSubmissionDto verify(UUID submissionId, VoteSubmissionVerifyRequest req);
    void delete(UUID submissionId);
    VoteSubmissionDto get(UUID submissionId);
    Page<VoteSubmissionDto> search(UUID orgId, UUID electionId, UUID centerId, UUID agentId,
                                   VoteStatus status, LocalDateTime from, LocalDateTime to, String q,
                                   Pageable pageable);

}