package Backend.ElectionVote.views.mapper;


import Backend.ElectionVote.views.dto.ElectionStatsOfficialDto;
import Backend.ElectionVote.views.entity.ElectionStatsOfficial;

public class ElectionStatsOfficialMapper {
    public ElectionStatsOfficialDto toDto(ElectionStatsOfficial e) {
        if (e == null) return null;
        ElectionStatsOfficialDto d = new ElectionStatsOfficialDto();
        d.setElectionId(e.getElectionId());
        d.setRegisteredVoters(e.getRegisteredVoters());
        d.setBallotsCast(e.getBallotsCast());
        d.setValidVotes(e.getValidVotes());
        d.setInvalidTotal(e.getInvalidTotal());
        d.setTurnoutPct(e.getTurnoutPct());
        d.setInvalidPct(e.getInvalidPct());
        return d;
    }
}