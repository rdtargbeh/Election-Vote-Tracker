package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.*;
import Backend.ElectionVote.entity.District;
import Backend.ElectionVote.entity.PollingCenter;
import Backend.ElectionVote.mapper.PollingCenterMapper;
import Backend.ElectionVote.repository.DistrictRepository;
import Backend.ElectionVote.repository.PollingCenterRepository;
import Backend.ElectionVote.service.PollingCenterService;
import Backend.ElectionVote.utility.PollingCenterSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class PollingCenterServiceImplementation implements PollingCenterService {

    private final PollingCenterRepository repository;
    private final DistrictRepository districtRepository;
    private final PollingCenterMapper mapper = new PollingCenterMapper();

    @Override
    public PollingCenterDto create(PollingCenterCreateRequest req) {
        if (repository.existsByCodeIgnoreCase(req.getCode())) {
            throw new ResponseStatusException(CONFLICT, "Polling center code already exists");
        }
        if (req.getRegisteredVoters() < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "registeredVoters cannot be negative");
        }

        District district = districtRepository.findById(req.getDistrictId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "District not found"));

        PollingCenter saved = repository.save(mapper.toEntity(req, district));
        return mapper.toDTO(saved);
    }

    @Override
    public PollingCenterDto update(UUID id, PollingCenterUpdateRequest req) {
        PollingCenter entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling center not found"));

        // if code is changing, enforce uniqueness
        if (req.getCode() != null && !req.getCode().equalsIgnoreCase(entity.getCode())
                && repository.existsByCodeIgnoreCase(req.getCode())) {
            throw new ResponseStatusException(CONFLICT, "Polling center code already exists");
        }
        if (req.getRegisteredVoters() != null && req.getRegisteredVoters() < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "registeredVoters cannot be negative");
        }

        District newDistrict = null;
        if (req.getDistrictId() != null) {
            newDistrict = districtRepository.findById(req.getDistrictId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "District not found"));
        }

        mapper.apply(req, entity, newDistrict);
        return mapper.toDTO(repository.save(entity));
    }

    @Override
    public void delete(UUID id) {
        // (Optional) Protect if referenced by results/allocations
        // Consider soft-delete or check foreign key refs if needed.
        if (!repository.existsById(id)) throw new ResponseStatusException(NOT_FOUND, "Polling center not found");
        repository.deleteById(id);
    }

    @Override
    public PollingCenterDto get(UUID id) {
        return repository.findById(id).map(mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling center not found"));
    }

    @Override
    public Page<PollingCenterDto> search(String q, UUID countyId, UUID districtId, Pageable pageable) {
        Specification<PollingCenter> spec = Specification
                .where(PollingCenterSpecs.textContains(q))
                .and(PollingCenterSpecs.countyEquals(countyId))
                .and(PollingCenterSpecs.districtEquals(districtId));

        return repository.findAll(spec, pageable).map(mapper::toDTO);
    }
}
