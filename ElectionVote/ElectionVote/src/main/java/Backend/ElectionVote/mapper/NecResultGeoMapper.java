package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.NecResultGeoDto;
import Backend.ElectionVote.views.NecResultGeo;
import org.springframework.stereotype.Component;

@Component
public final class NecResultGeoMapper {

    public static NecResultGeoDto toDTO(NecResultGeo g) {
        return NecResultGeoDto.builder()
                .resultId(g.getResultId())
                .electionId(g.getElectionId())
                .countyId(g.getCountyId()).countyName(g.getCountyName())
                .districtId(g.getDistrictId()).districtName(g.getDistrictName())
                .centerId(g.getCenterId()).centerCode(g.getCenterCode()).centerName(g.getCenterName())
                .ballotsCast(g.getBallotsCast())
                .totalRegisteredVoters(g.getTotalRegisteredVoters())
                .candidateVotesJson(g.getCandidateVotes())
                .uploadTime(g.getUploadTime())
                .source(g.getSource())
                .build();
    }
}
