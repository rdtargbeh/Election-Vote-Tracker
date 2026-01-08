package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.VoteSubmissionCreateRequest;
import Backend.ElectionVote.dto.VoteSubmissionDto;
import Backend.ElectionVote.dto.VoteSubmissionUpdateRequest;
import Backend.ElectionVote.dto.VoteSubmissionVerifyRequest;
import Backend.ElectionVote.entity.PollingCenterAllocation;
import Backend.ElectionVote.entity.PollingPlaceAllocation;
import Backend.ElectionVote.enums.ContestCategory;
import Backend.ElectionVote.enums.ContestScopeType;
import Backend.ElectionVote.enums.VoteStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteSubmissionService {


    VoteSubmissionDto create(VoteSubmissionCreateRequest req, HttpServletRequest request);
    VoteSubmissionDto create(VoteSubmissionCreateRequest req, List<MultipartFile> files, HttpServletRequest request);


    VoteSubmissionDto update(UUID id, VoteSubmissionUpdateRequest req, List<MultipartFile> files); // NEW

    VoteSubmissionDto verify(UUID id, VoteSubmissionVerifyRequest req);

    void delete(UUID id);

    VoteSubmissionDto get(UUID id);

    public Page<VoteSubmissionDto> search(
            UUID orgId,
            UUID electionId,
            UUID centerId,
            UUID agentId,
            VoteStatus status,
            LocalDateTime from,
            LocalDateTime to,
            String q,
            ContestCategory category,
            ContestScopeType scopeType,
            UUID countyId,
            UUID districtId,
            UUID contestId,          // ✅ ADD THIS (contest dropdown filter)
            Pageable pageable
    );


    long countVisibleSubmissions();



}