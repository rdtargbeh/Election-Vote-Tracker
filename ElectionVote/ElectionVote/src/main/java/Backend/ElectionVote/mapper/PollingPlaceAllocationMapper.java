package Backend.ElectionVote.mapper;


import Backend.ElectionVote.dto.PollingPlaceAllocationDto;
import Backend.ElectionVote.dto.PollingPlaceAllocationCreateRequest;
import Backend.ElectionVote.dto.PollingPlaceAllocationUpdateRequest;
import Backend.ElectionVote.entity.Election;
import Backend.ElectionVote.entity.PollingCenter;
import Backend.ElectionVote.entity.PollingPlace;
import Backend.ElectionVote.entity.PollingPlaceAllocation;

public class PollingPlaceAllocationMapper {

    public PollingPlaceAllocation toEntity(PollingPlaceAllocationCreateRequest req,
                                           Election election,
                                           PollingPlace place) {
        return PollingPlaceAllocation.builder()
                .election(election)
                .pollingPlace(place)
                .registeredVoters(req.getRegisteredVoters())
                .ballotsIssued(req.getBallotsIssued() != null ? req.getBallotsIssued() : 0)
                .build();
    }

    public void apply(PollingPlaceAllocationUpdateRequest req,
                      PollingPlaceAllocation entity) {
        if (req.getRegisteredVoters() != null) {
            entity.setRegisteredVoters(req.getRegisteredVoters());
        }
        if (req.getBallotsIssued() != null) {
            entity.setBallotsIssued(req.getBallotsIssued());
        }
    }

    public PollingPlaceAllocationDto toDTO(PollingPlaceAllocation a) {
        Election e = a.getElection();
        PollingPlace p = a.getPollingPlace();
        PollingCenter c = p.getPollingCenter();

        return PollingPlaceAllocationDto.builder()
                .placeAllocationId(a.getPlaceAllocationId())

                .electionId(e.getElectionId())
                .electionName(e.getElectionName())
                .year(e.getYear())

                .placeId(p.getPlaceId())
                .placeCode(p.getCode())
                .placeNumber(p.getPlaceNumber())
                .placeLabel(p.getLabel())

                .centerId(c.getCenterId())
                .centerCode(c.getCode())
                .centerName(c.getCenterName())

                .registeredVoters(a.getRegisteredVoters())
                .ballotsIssued(a.getBallotsIssued())
                .build();
    }
}
