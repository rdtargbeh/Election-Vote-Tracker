package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.ElectionPartyAssignRequest;
import Backend.ElectionVote.dto.ElectionPartyDto;
import Backend.ElectionVote.dto.ElectionPartyUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface ElectionPartyService {

    ElectionPartyDto addPartyToElection(ElectionPartyAssignRequest req);

    ElectionPartyDto updateElectionParty(UUID electionId, UUID partyId, ElectionPartyUpdateRequest req);

    void removePartyFromElection(UUID electionId, UUID partyId);

    List<ElectionPartyDto> listPartiesForElection(UUID electionId);

    ElectionPartyDto setQualificationStatus(
            UUID electionId,
            UUID partyId,
            boolean isQualified
    );



}
