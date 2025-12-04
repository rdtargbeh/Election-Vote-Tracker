package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.VoteSubmissionCreateRequest;
import Backend.ElectionVote.dto.VoteSubmissionDto;
import Backend.ElectionVote.dto.VoteSubmissionUpdateRequest;
import Backend.ElectionVote.dto.VoteSubmissionVerifyRequest;
import Backend.ElectionVote.enums.VoteStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface VoteSubmissionService {

//    VoteSubmissionDto create(VoteSubmissionCreateRequest req);
//    VoteSubmissionDto create(VoteSubmissionCreateRequest req, List<MultipartFile> files); // NEW


    // New overloads that accept the HttpServletRequest so server can set clientIp/userAgent and optionally geolocation
    VoteSubmissionDto create(VoteSubmissionCreateRequest req, HttpServletRequest request);
    VoteSubmissionDto create(VoteSubmissionCreateRequest req, List<MultipartFile> files, HttpServletRequest request);


//    VoteSubmissionDto update(UUID id, VoteSubmissionUpdateRequest req);
    VoteSubmissionDto update(UUID id, VoteSubmissionUpdateRequest req, List<MultipartFile> files); // NEW

    VoteSubmissionDto verify(UUID id, VoteSubmissionVerifyRequest req);

    void delete(UUID id);

    VoteSubmissionDto get(UUID id);

    Page<VoteSubmissionDto> search(UUID orgId, UUID electionId, UUID centerId, UUID agentId,
                                   VoteStatus status, LocalDateTime from, LocalDateTime to, String q,
                                   Pageable pageable);

    long countVisibleSubmissions();



}