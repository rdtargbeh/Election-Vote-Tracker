package Backend.ElectionVote.mapper;


import Backend.ElectionVote.dto.DiscrepancyDto;
import Backend.ElectionVote.entity.County;
import Backend.ElectionVote.entity.Discrepancy;
import Backend.ElectionVote.entity.District;
import Backend.ElectionVote.entity.PollingCenter;

public class DiscrepancyMapper {
    public DiscrepancyDto toDTO(Discrepancy d) {
        PollingCenter pc = d.getPollingCenter();
        District dist = pc != null ? pc.getDistrict() : null;
        County county = dist != null ? dist.getCounty() : null;

        return DiscrepancyDto.builder()
                .discId(d.getDiscId())
                .electionId(d.getElection().getElectionId())
                .centerId(pc != null ? pc.getCenterId() : null)
                .centerCode(pc != null ? pc.getCode() : null)
                .centerName(pc != null ? pc.getCenterName() : null)
                .districtId(dist != null ? dist.getDistrictId() : null)
                .districtName(dist != null ? dist.getDistrictName() : null)
                .countyId(county != null ? county.getCountyId() : null)
                .countyName(county != null ? county.getCountyName() : null)
                .orgId(d.getOrganization() != null ? d.getOrganization().getOrgId() : null)
                .partyValid(d.getPartyValid())
                .officialValid(d.getOfficialValid())
                .partyInvalid(d.getPartyInvalid())
                .officialInvalid(d.getOfficialInvalid())
                .deltaValid(d.getDeltaValid())
                .deltaInvalid(d.getDeltaInvalid())
                .status(d.getStatus())
                .notedAt(d.getNotedAt())
                .build();
    }
}
