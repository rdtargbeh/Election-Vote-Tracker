package Backend.ElectionVote.mapper;


import Backend.ElectionVote.dto.ContestCreateRequest;
import Backend.ElectionVote.dto.ContestDto;
import Backend.ElectionVote.dto.ContestUpdateRequest;
import Backend.ElectionVote.entity.Contest;
import org.springframework.stereotype.Component;

@Component
public class ContestMapper {

    public ContestDto toDto(Contest c) {
        if (c == null) return null;
        ContestDto d = new ContestDto();
        d.setContestId(c.getContestId());
        d.setElectionId(c.getElectionId());
        d.setContestName(c.getContestName());

        d.setCategory(c.getCategory());
        d.setScopeType(c.getScopeType());
        d.setCountyId(c.getCountyId());
        d.setDistrictId(c.getDistrictId());

        d.setVoteMethod(c.getVoteMethod());
        d.setSeats(c.getSeats());
        d.setMaxSelections(c.getMaxSelections());

        d.setDescription(c.getDescription());

        d.setStatus(c.getStatus());
        d.setIsActive(c.isActive());

        d.setDateCreated(c.getDateCreated());
        d.setDateUpdated(c.getDateUpdated());
        return d;
    }

    public Contest toEntity(ContestCreateRequest req) {
        Contest c = new Contest();
        c.setElectionId(req.getElectionId());
        c.setContestName(req.getContestName());

        c.setCategory(req.getCategory());
        c.setScopeType(req.getScopeType());
        c.setCountyId(req.getCountyId());
        c.setDistrictId(req.getDistrictId());

        c.setVoteMethod(req.getVoteMethod());
        c.setSeats(req.getSeats() == null ? 1 : req.getSeats());
        c.setMaxSelections(req.getMaxSelections() == null ? 1 : req.getMaxSelections());

        c.setDescription(req.getDescription());

        c.setStatus(req.getStatus());
        c.setActive(req.getIsActive() == null || req.getIsActive());
        return c;
    }

    public void apply(ContestUpdateRequest req, Contest c) {
        if (req.getElectionId() != null) c.setElectionId(req.getElectionId());
        if (req.getContestName() != null) c.setContestName(req.getContestName());

        if (req.getCategory() != null) c.setCategory(req.getCategory());
        if (req.getScopeType() != null) c.setScopeType(req.getScopeType());
        if (req.getCountyId() != null || req.getScopeType() != null) c.setCountyId(req.getCountyId());
        if (req.getDistrictId() != null || req.getScopeType() != null) c.setDistrictId(req.getDistrictId());

        if (req.getVoteMethod() != null) c.setVoteMethod(req.getVoteMethod());
        if (req.getSeats() != null) c.setSeats(req.getSeats());
        if (req.getMaxSelections() != null) c.setMaxSelections(req.getMaxSelections());

        if (req.getDescription() != null) c.setDescription(req.getDescription());

        if (req.getStatus() != null) c.setStatus(req.getStatus());
        if (req.getIsActive() != null) c.setActive(req.getIsActive());
    }
}
