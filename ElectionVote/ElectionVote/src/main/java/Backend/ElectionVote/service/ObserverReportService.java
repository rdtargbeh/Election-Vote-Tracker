package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.ObserverReportCreateRequest;
import Backend.ElectionVote.dto.ObserverReportDto;
import Backend.ElectionVote.dto.ObserverReportUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ObserverReportService {

        ObserverReportDto create(ObserverReportCreateRequest req);

        ObserverReportDto update(UUID reportId, ObserverReportUpdateRequest req);

        void delete(UUID reportId);

        ObserverReportDto get(UUID reportId);

        Page<ObserverReportDto> search(UUID orgId, UUID observerId, UUID countyId, UUID centerId,
                                       String type, Boolean resolved, LocalDateTime from, LocalDateTime to,
                                       String q, Pageable pageable);

        // Optional: spatial quick search (non-pageable)
        java.util.List<ObserverReportDto> near(UUID orgId, double lat, double lon, double meters);

}
