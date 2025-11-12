package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.DiscrepancyDto;
import Backend.ElectionVote.dto.DiscrepancySearchRequest;
import Backend.ElectionVote.enums.DiscrepancyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;


public interface DiscrepancyService {
    Page<DiscrepancyDto> search(DiscrepancySearchRequest req, Pageable pageable);
    DiscrepancyDto get(UUID id);
    DiscrepancyDto updateStatus(UUID id, DiscrepancyStatus status);

    /** Recompute differences vs NEC for a given org & election (upsert/open/resolve). */
    List<DiscrepancyDto> reconcile(UUID orgId, UUID electionId);
}
