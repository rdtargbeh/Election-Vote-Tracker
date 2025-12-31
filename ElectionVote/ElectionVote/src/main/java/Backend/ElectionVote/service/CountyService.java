package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.CountyDto;
import Backend.ElectionVote.dto.CountyRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CountyService {
    CountyDto create(CountyRequest req);
    CountyDto update(UUID countyId, CountyRequest req);
    void delete(UUID countyId);
    CountyDto get(UUID countyId);
    Page<CountyDto> list(String q, Pageable pageable);
}