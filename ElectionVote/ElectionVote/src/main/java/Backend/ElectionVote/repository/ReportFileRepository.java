package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.ReportFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportFileRepository extends JpaRepository<ReportFile, UUID> {
}