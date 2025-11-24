package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.PollingPlaceCreateRequest;
import Backend.ElectionVote.dto.PollingPlaceDto;

import java.util.List;
import java.util.UUID;

public interface PollingPlaceService {

    PollingPlaceDto create(PollingPlaceCreateRequest req);

    PollingPlaceDto get(UUID id);

    List<PollingPlaceDto> listByCenter(UUID centerId);

    PollingPlaceDto deactivate(UUID id);
}

