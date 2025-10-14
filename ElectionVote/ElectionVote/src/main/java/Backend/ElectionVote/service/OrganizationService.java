package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.OrganizationCreateRequest;
import Backend.ElectionVote.dto.OrganizationDto;
import Backend.ElectionVote.dto.OrganizationUpdateRequest;
import Backend.ElectionVote.uility.OrganizationSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationService {
    OrganizationDto create(OrganizationCreateRequest req);

    Optional<OrganizationDto> get(UUID orgId);

    Page<OrganizationDto> search(OrganizationSearchRequest req, Pageable pageable);

    OrganizationDto update(UUID orgId, OrganizationUpdateRequest req);

    void setActive(UUID orgId, boolean active);

    void assignParty(UUID orgId, UUID partyId);        // pass null to clear

    Optional<OrganizationDto> getBySubdomain(String subdomain);
}
