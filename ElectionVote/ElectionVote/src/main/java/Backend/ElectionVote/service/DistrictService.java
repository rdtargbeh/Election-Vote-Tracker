package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.DistrictDto;
import Backend.ElectionVote.dto.DistrictRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DistrictService {

    DistrictDto create(DistrictRequest req);
    DistrictDto update(UUID districtId, DistrictRequest req);
    void delete(UUID districtId);
    DistrictDto get(UUID districtId);
    List<DistrictDto> listByCounty(UUID countyId);

    /**
     * Optional filters:
     * - q: name contains (case-insensitive)
     * - countyId: restrict to a county
     */
    Page<DistrictDto> list(String q, UUID countyId, Pageable pageable);
}

