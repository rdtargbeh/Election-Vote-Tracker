package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.PollingPlaceCreateRequest;
import Backend.ElectionVote.dto.PollingPlaceDto;
import Backend.ElectionVote.dto.PollingPlaceUpdateRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface PollingPlaceService {

    PollingPlaceDto create(PollingPlaceCreateRequest req);

    PollingPlaceDto update(UUID id, PollingPlaceUpdateRequest req);

    PollingPlaceDto get(UUID id);

    List<PollingPlaceDto> listByCenter(UUID centerId);

    PollingPlaceDto setActive(UUID id, boolean active);

    void delete(UUID id);

    Page<PollingPlaceDto> list(int page, int size, String q,
                               UUID countyId, UUID districtId, UUID centerId,
                               Boolean active);


}

