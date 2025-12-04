package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.CandidateCreateRequest;
import Backend.ElectionVote.dto.CandidateDto;
import Backend.ElectionVote.dto.CandidateUpdateRequest;
import Backend.ElectionVote.entity.Candidate;
import Backend.ElectionVote.entity.Party;
import org.springframework.stereotype.Component;

@Component
public class CandidateMapper {

    public CandidateDto toDTO(Candidate entity) {
        if (entity == null) return null;

        CandidateDto dto = new CandidateDto();
        dto.setCandidateId(entity.getCandidateId());
        dto.setFullName(entity.getFullName());
        dto.setPosition(entity.getPosition());
        dto.setPhotoUrl(entity.getPhotoUrl());
        dto.setActive(entity.isActive());
        dto.setIndependent(entity.isIndependent());

        if (entity.getParty() != null) {
            dto.setPartyId(entity.getParty().getPartyId());
            dto.setPartyName(entity.getParty().getPartyName());
            dto.setAbbreviation(entity.getParty().getAbbreviation());
        }

        dto.setDateCreated(entity.getDateCreated());
        dto.setDateUpdated(entity.getDateUpdated());

        return dto;
    }


    public Candidate toEntity(CandidateCreateRequest req, Party party) {
        if (req == null) return null;

        Candidate c = new Candidate();
        c.setFullName(req.getFullName());
        c.setPosition(req.getPosition());
        c.setPhotoUrl(req.getPhotoUrl());
        c.setActive(true); // default on create

        // Default independent=false unless explicitly true
        boolean isIndependent = Boolean.TRUE.equals(req.getIndependent());
        c.setIndependent(isIndependent);

        // If independent, ensure no party attached at entity level (extra safety)
        if (isIndependent) {
            c.setParty(null);
        } else {
            c.setParty(party);
        }


        return c;
    }

    public void apply(CandidateUpdateRequest req, Candidate entity, Party party) {
        if (req == null || entity == null) return;

        if (req.getFullName() != null) entity.setFullName(req.getFullName());
        if (req.getPosition() != null) entity.setPosition(req.getPosition());
        if (req.getPhotoUrl() != null) entity.setPhotoUrl(req.getPhotoUrl());
        if (req.getIsActive() != null) entity.setActive(req.getIsActive());

        if (req.getIndependent() != null) {
            entity.setIndependent(req.getIndependent());
            // If switching to independent, clear party
            if (req.getIndependent()) {
                entity.setParty(null);
            }
        }

        // If not independent & caller explicitly gave a partyId, attach party
        if (party != null && !entity.isIndependent()) {
            entity.setParty(party);
        }
    }


}