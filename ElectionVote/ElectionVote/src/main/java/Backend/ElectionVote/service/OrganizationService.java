package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.OrganizationBrandingUpdateRequest;
import Backend.ElectionVote.dto.OrganizationCreateRequest;
import Backend.ElectionVote.dto.OrganizationDto;
import Backend.ElectionVote.dto.OrganizationUpdateRequest;
import Backend.ElectionVote.utility.OrganizationSearchRequest;
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

    Optional<OrganizationDto> getBySubdomain(String subdomain);

    OrganizationDto updateBranding(UUID id, OrganizationBrandingUpdateRequest req);
}
