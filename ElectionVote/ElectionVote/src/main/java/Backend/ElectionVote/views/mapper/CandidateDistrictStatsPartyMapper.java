package Backend.ElectionVote.views.mapper;


import Backend.ElectionVote.views.dto.CandidateDistrictStatsPartyDto;
import Backend.ElectionVote.views.entity.CandidateDistrictStatsParty;

public class CandidateDistrictStatsPartyMapper {
    public CandidateDistrictStatsPartyDto toDto(CandidateDistrictStatsParty e) {
        if (e == null) return null;
        CandidateDistrictStatsPartyDto d = new CandidateDistrictStatsPartyDto();
        if (e.getId() != null) {
            d.setOrgId(e.getId().getOrgId());
            d.setElectionId(e.getId().getElectionId());
            d.setDistrictId(e.getId().getDistrictId());
            d.setCandidateId(e.getId().getCandidateId());
        }
        d.setCountyId(e.getCountyId());
        d.setCountyName(e.getCountyName());
        d.setDistrictName(e.getDistrictName());
        d.setCandidateName(e.getCandidateName());
        d.setPartyId(e.getPartyId());
        d.setPartyName(e.getPartyName());
        d.setAbbreviation(e.getAbbreviation());
        d.setCandidateVotes(e.getCandidateVotes());
        d.setBallotsCast(e.getBallotsCast());
        d.setTotalValidVotes(e.getTotalValidVotes());
        d.setTotalInvalidVotes(e.getTotalInvalidVotes());
        d.setVoteSharePct(e.getVoteSharePct());
        return d;
    }
}