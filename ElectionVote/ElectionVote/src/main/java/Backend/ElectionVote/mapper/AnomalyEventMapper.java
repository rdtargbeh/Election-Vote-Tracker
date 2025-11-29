package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.AnomalyEventCreateRequest;
import Backend.ElectionVote.dto.AnomalyEventDto;
import Backend.ElectionVote.dto.AnomalyEventUpdateRequest;
import Backend.ElectionVote.entity.AnomalyEvent;
import Backend.ElectionVote.entity.Election;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.PollingCenter;
import org.springframework.stereotype.Component;

// mapper/AnomalyEventMapper.java
@Component
public class AnomalyEventMapper {

    public AnomalyEvent toEntity(AnomalyEventCreateRequest req, Organization org, Election election, PollingCenter center) {
        AnomalyEvent a = new AnomalyEvent();
        a.setOrganization(org);
        a.setElection(election);
        a.setPollingCenter(center);
        a.setKind(req.getKind());
        a.setDetails(req.getDetails());
        return a;
    }

    public void apply(AnomalyEventUpdateRequest req, AnomalyEvent entity, PollingCenter center) {
        if (req.getKind() != null) entity.setKind(req.getKind());
        if (req.getDetails() != null) entity.setDetails(req.getDetails());
        if (center != null) entity.setPollingCenter(center);
    }

    public AnomalyEventDto toDTO(AnomalyEvent a) {
        return AnomalyEventDto.builder()
                .anomalyId(a.getAnomalyId())
                .orgId(a.getOrganization().getOrgId())
                .electionId(a.getElection().getElectionId())
                .centerId(a.getPollingCenter() != null ? a.getPollingCenter().getCenterId() : null)
                .kind(a.getKind())
                .details(a.getDetails())
                .dateCreated(a.getDateCreated())
                .build();
    }
}

