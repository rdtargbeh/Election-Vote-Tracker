package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.*;
import Backend.ElectionVote.entity.*;
import Backend.ElectionVote.mapper.ObserverReportMapper;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.service.ObserverReportService;
import Backend.ElectionVote.utility.ObserverReportSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class ObserverReportServiceImplementation implements ObserverReportService {

    private final ObserverReportRepository repo;
    private final OrganizationRepository orgRepo;
    private final SystemUserRepository userRepo;
    private final CountyRepository countyRepo;
    private final PollingCenterRepository centerRepo;

    private final ObserverReportMapper mapper = new ObserverReportMapper();


    @Override
    public ObserverReportDto create(ObserverReportCreateRequest req) {
        Organization org = orgRepo.findById(req.getOrgId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));
        SystemUser observer = userRepo.findById(req.getObserverId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Observer not found"));

        County county = null;
        PollingCenter center = null;

        if (req.getCenterId() != null) {
            center = centerRepo.findById(req.getCenterId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling center not found"));
            // If county not provided, derive from center
            county = center.getDistrict().getCounty();
        } else if (req.getCountyId() != null) {
            county = countyRepo.findById(req.getCountyId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "County not found"));
        }

        if ((req.getLatitude() == null) ^ (req.getLongitude() == null)) {
            throw new ResponseStatusException(BAD_REQUEST, "Both latitude and longitude are required for GPS");
        }

        ObserverReport saved = repo.save(mapper.toEntity(req, org, observer, county, center));
        return mapper.toDTO(saved);
    }


    @Override
    public ObserverReportDto update(UUID reportId, ObserverReportUpdateRequest req) {
        ObserverReport entity = repo.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Report not found"));

        County newCounty = null;
        PollingCenter newCenter = null;

        if (req.getCenterId() != null) {
            newCenter = centerRepo.findById(req.getCenterId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Polling center not found"));
            newCounty = newCenter.getDistrict().getCounty(); // keep them consistent
        } else if (req.getCountyId() != null) {
            newCounty = countyRepo.findById(req.getCountyId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "County not found"));
        }

        if ((req.getLatitude() == null) ^ (req.getLongitude() == null)) {
            throw new ResponseStatusException(BAD_REQUEST, "Both latitude and longitude are required for GPS");
        }

        mapper.apply(req, entity, newCounty, newCenter);
        return mapper.toDTO(repo.save(entity));
    }


    @Override
    public void delete(UUID reportId) {
        if (!repo.existsById(reportId)) throw new ResponseStatusException(NOT_FOUND, "Report not found");
        repo.deleteById(reportId);
    }

    @Override
    public ObserverReportDto get(UUID reportId) {
        return repo.findById(reportId).map(mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Report not found"));
    }

    @Override
    public Page<ObserverReportDto> search(UUID orgId, UUID observerId, UUID countyId, UUID centerId,
                                          String type, Boolean resolved, LocalDateTime from, LocalDateTime to,
                                          String q, Pageable pageable) {
        Specification<ObserverReport> spec = Specification
                .where(ObserverReportSpecs.orgEquals(orgId))
                .and(ObserverReportSpecs.observerEquals(observerId))
                .and(ObserverReportSpecs.countyEquals(countyId))
                .and(ObserverReportSpecs.centerEquals(centerId))
                .and(ObserverReportSpecs.typeEquals(type))
                .and(ObserverReportSpecs.resolvedEquals(resolved))
                .and(ObserverReportSpecs.between(from, to))
                .and(ObserverReportSpecs.textSearch(q));

        return repo.findAll(spec, pageable).map(mapper::toDTO);
    }

    @Override
    public java.util.List<ObserverReportDto> near(UUID orgId, double lat, double lon, double meters) {
        return repo.findNear(orgId, lon, lat, meters).stream().map(mapper::toDTO).toList();
    }

}