package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.CandidateCreateRequest;
import Backend.ElectionVote.dto.CandidateDto;
import Backend.ElectionVote.dto.CandidateUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CandidateService {
    CandidateDto create(CandidateCreateRequest req);
    CandidateDto update(UUID id, CandidateUpdateRequest req);
    void delete(UUID id);
    CandidateDto get(UUID id);
    Page<CandidateDto> search(String q, String position, UUID partyId, Boolean active, Pageable pageable);
}