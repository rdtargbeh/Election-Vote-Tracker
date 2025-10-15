package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.PartyCreateRequest;
import Backend.ElectionVote.dto.PartyDto;
import Backend.ElectionVote.dto.PartyUpdateRequest;
import Backend.ElectionVote.uility.PartySearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PartyService {

    PartyDto create(PartyCreateRequest req);

    Optional<PartyDto> get(UUID partyId);

    Optional<PartyDto> getByAbbreviation(String abbrev);

    Page<PartyDto> search(PartySearchRequest req, Pageable pageable);

    PartyDto update(UUID partyId, PartyUpdateRequest req);

    void delete(UUID partyId); // guarded if referenced
}
