package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.ElectionCandidateCreateRequest;
import Backend.ElectionVote.dto.ElectionCandidateDto;
import Backend.ElectionVote.dto.ElectionCandidateUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ElectionCandidateService {
    ElectionCandidateDto create(ElectionCandidateCreateRequest req);

    ElectionCandidateDto update(UUID id, ElectionCandidateUpdateRequest req);

    void delete(UUID id);

    ElectionCandidateDto get(UUID id);

    Page<ElectionCandidateDto> getAll(Pageable pageable);
}
