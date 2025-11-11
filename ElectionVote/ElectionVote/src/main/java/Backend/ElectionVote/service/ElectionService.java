package Backend.ElectionVote.service;


import Backend.ElectionVote.dto.ElectionCreateRequest;
import Backend.ElectionVote.dto.ElectionDto;
import Backend.ElectionVote.dto.ElectionSearchRequest;
import Backend.ElectionVote.dto.ElectionUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ElectionService {

    ElectionDto create(ElectionCreateRequest req);
    ElectionDto update(UUID id, ElectionUpdateRequest req);
    void delete(UUID id);
    ElectionDto get(UUID id);
    Page<ElectionDto> search(ElectionSearchRequest req, Pageable pageable);

}
