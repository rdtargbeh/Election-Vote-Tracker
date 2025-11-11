package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.CandidateCreateRequest;
import Backend.ElectionVote.dto.CandidateDto;
import Backend.ElectionVote.dto.CandidateUpdateRequest;
import Backend.ElectionVote.entity.Candidate;
import Backend.ElectionVote.entity.Party;
import org.springframework.stereotype.Component;

@Component
public class CandidateMapper {

    public CandidateDto toDTO(Candidate c) {
        if (c == null) return null;
        return CandidateDto.builder()
                .candidateId(c.getCandidateId())
                .fullName(c.getFullName())
                .position(c.getPosition())
                .partyId(c.getParty() != null ? c.getParty().getPartyId() : null)
                .partyName(c.getParty() != null ? c.getParty().getPartyName() : null)
                .partyAbbreviation(c.getParty() != null ? c.getParty().getAbbreviation() : null)
                .photoUrl(c.getPhotoUrl())
                .isActive(c.isActive())
                .build();
    }

    public Candidate toEntity(CandidateCreateRequest req, Party party) {
        if (req == null) return null;
        Candidate c = new Candidate();
        c.setFullName(req.getFullName());
        c.setPosition(req.getPosition());
        c.setParty(party);
        c.setPhotoUrl(req.getPhotoUrl());
        c.setActive(req.isActive());
        return c;
    }

    public void apply(CandidateUpdateRequest req, Candidate c, Party newPartyOrNull) {
        if (req == null || c == null) return;
        if (req.getFullName() != null) c.setFullName(req.getFullName());
        if (req.getPosition() != null) c.setPosition(req.getPosition());
        if (req.getPhotoUrl() != null) c.setPhotoUrl(req.getPhotoUrl());
        if (req.getIsActive() != null) c.setActive(req.getIsActive());
        if (req.getPartyId() != null)  c.setParty(newPartyOrNull); // resolve in service
    }
}