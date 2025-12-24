package Backend.ElectionVote.service;


import Backend.ElectionVote.dto.ReportRequest;
import Backend.ElectionVote.dto.ReportSnapshotDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReportService {
    UUID createReport(ReportRequest req, String requestedBy);
    ReportSnapshotDto getSnapshot(UUID snapshotId);
    Page<ReportSnapshotDto> listSnapshots(Pageable pageable);
    byte[] downloadReport(UUID snapshotId) throws Exception;
}
