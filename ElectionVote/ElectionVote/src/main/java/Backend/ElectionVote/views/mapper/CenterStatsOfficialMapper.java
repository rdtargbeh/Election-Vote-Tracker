package Backend.ElectionVote.views.mapper;

import Backend.ElectionVote.views.dto.CenterStatsOfficialDto;
import Backend.ElectionVote.views.entity.CenterStatsOfficial;

;

public class CenterStatsOfficialMapper {
    public CenterStatsOfficialDto toDto(CenterStatsOfficial e) {
        if (e == null) return null;
        CenterStatsOfficialDto d = new CenterStatsOfficialDto();
        if (e.getId() != null) {
            d.setElectionId(e.getId().getElectionId());
            d.setCenterId(e.getId().getCenterId());
        }
        d.setCenterCode(e.getCenterCode());
        d.setCenterName(e.getCenterName());
        d.setDistrictId(e.getDistrictId());
        d.setDistrictName(e.getDistrictName());
        d.setCountyId(e.getCountyId());
        d.setCountyName(e.getCountyName());
        d.setRegisteredVoters(e.getRegisteredVoters());
        d.setBallotsCast(e.getBallotsCast());
        d.setValidVotes(e.getValidVotes());
        d.setInvalidTotal(e.getInvalidTotal());
        d.setTurnoutPct(e.getTurnoutPct());
        d.setInvalidPct(e.getInvalidPct());
        return d;
    }
}