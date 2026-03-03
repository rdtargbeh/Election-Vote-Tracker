package Backend.ElectionVote.service.implement;


import Backend.ElectionVote.dto.AnomalyEventCreateRequest;
import Backend.ElectionVote.dto.AnomalyEventDto;
import Backend.ElectionVote.dto.AnomalyEventUpdateRequest;
import Backend.ElectionVote.entity.AnomalyEvent;
import Backend.ElectionVote.entity.Election;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.PollingCenter;
import Backend.ElectionVote.enums.AnomalyKind;
import Backend.ElectionVote.mapper.AnomalyEventMapper;
import Backend.ElectionVote.repository.AnomalyEventRepository;
import Backend.ElectionVote.repository.ElectionRepository;
import Backend.ElectionVote.repository.OrganizationRepository;
import Backend.ElectionVote.repository.PollingCenterRepository;
import Backend.ElectionVote.service.AnomalyEventService;
import Backend.ElectionVote.utility.AnomalyEventSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AnomalyEventServiceImplementation implements AnomalyEventService {

    private final AnomalyEventRepository repo;
    private final OrganizationRepository orgRepo;
    private final ElectionRepository electionRepo;
    private final PollingCenterRepository centerRepo;
    private final AnomalyEventMapper mapper;

    @Override
    public AnomalyEventDto create(AnomalyEventCreateRequest req) {
        Organization org = orgRepo.findById(req.getOrgId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        Election election = electionRepo.findById(req.getElectionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Election not found"));

        PollingCenter center = null;
        if (req.getCenterId() != null) {
            center = centerRepo.findById(req.getCenterId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Polling center not found"));
        }

        // (Optional) simple de-dup window (e.g., 10 minutes)
        if (center != null && repo.existsRecentOfSameKindAtCenter(
                org.getOrgId(), election.getElectionId(), center.getCenterId(), req.getKind(),
                LocalDateTime.now().minusMinutes(10))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Similar anomaly recently recorded at this center");
        }

        AnomalyEvent saved = repo.save(mapper.toEntity(req, org, election, center));
        return mapper.toDTO(saved);
    }

    @Override
    public AnomalyEventDto update(UUID anomalyId, AnomalyEventUpdateRequest req) {
        AnomalyEvent entity = repo.findById(anomalyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anomaly not found"));

        PollingCenter center = null;
        if (req.getCenterId() != null) {
            center = centerRepo.findById(req.getCenterId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Polling center not found"));
        }

        mapper.apply(req, entity, center);
        return mapper.toDTO(repo.save(entity));
    }

    @Override
    public void delete(UUID anomalyId) {
        if (!repo.existsById(anomalyId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Anomaly not found");
        repo.deleteById(anomalyId);
    }

    @Override
    @Transactional(readOnly = true)
    public AnomalyEventDto get(UUID anomalyId) {
        return repo.findById(anomalyId).map(mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anomaly not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnomalyEventDto> search(UUID orgId, UUID electionId, UUID centerId,
                                        AnomalyKind kind, LocalDateTime from, LocalDateTime to,
                                        String q, Pageable pageable) {

        Specification<AnomalyEvent> spec = Specification
                .where(AnomalyEventSpecs.orgEquals(orgId))
                .and(AnomalyEventSpecs.electionEquals(electionId))
                .and(AnomalyEventSpecs.centerEquals(centerId))
                .and(AnomalyEventSpecs.kindEquals(kind))
                .and(AnomalyEventSpecs.between(from, to))
                .and(AnomalyEventSpecs.textSearch(q));

        return repo.findAll(spec, pageable).map(mapper::toDTO);
    }
}
