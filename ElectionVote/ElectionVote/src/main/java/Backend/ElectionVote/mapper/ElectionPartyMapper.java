package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.ElectionPartyDto;
import Backend.ElectionVote.entity.ElectionParty;
import org.springframework.stereotype.Component;

@Component
public class ElectionPartyMapper {

    public ElectionPartyDto toDto(ElectionParty ep) {
        if (ep == null) return null;

        ElectionPartyDto dto = new ElectionPartyDto();
        dto.setElectionId(ep.getElection().getElectionId());
        dto.setPartyId(ep.getParty().getPartyId());
        dto.setBallotOrder(ep.getBallotOrder());
        dto.setQualified(ep.isQualified());

        if (ep.getParty() != null) {
            dto.setPartyName(ep.getParty().getPartyName());
            dto.setPartyAbbreviation(ep.getParty().getAbbreviation());
            dto.setPartyLogoUrl(ep.getParty().getLogoUrl());
        }

        return dto;
    }
}
