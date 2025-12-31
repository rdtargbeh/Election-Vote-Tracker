package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.PollingPlaceCreateRequest;
import Backend.ElectionVote.dto.PollingPlaceDto;
import Backend.ElectionVote.entity.District;
import Backend.ElectionVote.entity.PollingCenter;
import Backend.ElectionVote.entity.PollingPlace;
import Backend.ElectionVote.mapper.PollingPlaceMapper;
import Backend.ElectionVote.repository.PollingCenterRepository;
import Backend.ElectionVote.repository.PollingPlaceRepository;
import Backend.ElectionVote.service.PollingPlaceService;
import Backend.ElectionVote.utility.PollingPlaceCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class PollingPlaceServiceImplementation implements PollingPlaceService {

    private final PollingPlaceRepository placeRepo;
    private final PollingCenterRepository centerRepo;
    private final PollingPlaceMapper mapper = new PollingPlaceMapper();

    // ---------------------------------------------------------------------
    // CREATE: auto-generate placeNumber + place code
    // ---------------------------------------------------------------------
    @Override
    @Transactional
    public PollingPlaceDto create(PollingPlaceCreateRequest req) {
        // 1) Load center
        PollingCenter center = centerRepo.findById(req.getCenterId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling center not found"));

        // 2) District needed for code generation
        District district = center.getDistrict();
        if (district == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Polling center is not linked to a district");
        }

        // 3) Determine next placeNumber inside this center (1, 2, 3, ...)
        List<PollingPlace> existing =
                placeRepo.findByPollingCenter_CenterIdOrderByPlaceNumberAsc(center.getCenterId());

        int nextNumber = existing.isEmpty()
                ? 1
                : existing.get(existing.size() - 1).getPlaceNumber() + 1;

        // 4) Generate unique place code: PP-DDD-CCC-RRRRR
        String code = generateUniquePlaceCode(district, center);

        // 5) Build & save entity
        PollingPlace entity = mapper.toEntity(req, center, nextNumber, code);
        PollingPlace saved = placeRepo.save(entity);

        return mapper.toDTO(saved);
    }

    /**
     * Generate a unique polling place code using district + center name.
     * Uses PollingPlaceCodeGenerator and checks DB collisions up to a few times.
     */
    private String generateUniquePlaceCode(District district, PollingCenter center) {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            String candidate = PollingPlaceCodeGenerator.generateCode(district, center);
            if (placeRepo.findByCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new ResponseStatusException(
                CONFLICT,
                "Failed to generate unique polling place code after several attempts"
        );
    }

    // ---------------------------------------------------------------------
    // GET BY ID
    // ---------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public PollingPlaceDto get(UUID id) {
        return placeRepo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling place not found"));
    }

    // ---------------------------------------------------------------------
    // LIST BY CENTER
    // ---------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<PollingPlaceDto> listByCenter(UUID centerId) {
        return placeRepo.findByPollingCenter_CenterIdOrderByPlaceNumberAsc(centerId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    // ---------------------------------------------------------------------
    // DEACTIVATE (soft-disable a place)
    // ---------------------------------------------------------------------
    @Override
    @Transactional
    public PollingPlaceDto deactivate(UUID id) {
        PollingPlace p = placeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling place not found"));
        p.setActive(false);
        PollingPlace saved = placeRepo.save(p);
        return mapper.toDTO(saved);
    }

    // ---------------------------------------------------------------------
    // DELETE (hard delete) + resequence placeNumber
    // ---------------------------------------------------------------------
    @Transactional
    public void delete(UUID id) {
        PollingPlace p = placeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling place not found"));

        UUID centerId = p.getPollingCenter().getCenterId();

        placeRepo.delete(p);

        // After deleting/merging, re-sequence placeNumber for remaining places (1,2,3,...)
        resequencePlaceNumbers(centerId);
    }

    /**
     * Re-orders placeNumber for all places in the center so they are:
     *    1, 2, 3, ...
     *
     * NOTE:
     *  - We do NOT change 'code' here. Codes remain stable so that historical
     *    references on tally sheets / NEC exports remain valid.
     *  - Currently this includes both active and inactive places. If you only
     *    want active places numbered, filter by place.isActive().
     */
    private void resequencePlaceNumbers(UUID centerId) {
        List<PollingPlace> places = placeRepo.findByPollingCenter_CenterIdOrderByPlaceNumberAsc(centerId);

        int seq = 1;
        for (PollingPlace place : places) {
            // If you want to skip inactive places, uncomment:
            // if (!place.isActive()) continue;

            if (place.getPlaceNumber() == null || place.getPlaceNumber() != seq) {
                place.setPlaceNumber(seq);
            }
            seq++;
        }

        placeRepo.saveAll(places);
    }
}
