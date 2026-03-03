package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.DistrictDto;
import Backend.ElectionVote.dto.DistrictRequest;
import Backend.ElectionVote.entity.County;
import Backend.ElectionVote.entity.District;
import org.springframework.stereotype.Component;

@Component
public class DistrictMapper {

    public DistrictDto toDTO(District d) {
        if (d == null) return null;
        return new DistrictDto(
                d.getDistrictId(),
                d.getDistrictName(),
                d.getCounty() != null ? d.getCounty().getCountyId() : null,
                d.getCounty() != null ? d.getCounty().getCountyName() : null
        );
    }

    /** county must be loaded by service and passed in */
    public District toEntity(DistrictRequest req, County county) {
        if (req == null) return null;
        return District.builder()
                .districtName(req.districtName())
                .county(county)
                .build();
    }

    public void updateEntity(District target, DistrictRequest req, County county) {
        if (target == null || req == null) return;
        if (req.districtName() != null) target.setDistrictName(req.districtName());
        if (county != null) target.setCounty(county);
    }
}
