package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.PollingCenter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PollingCenterRepository extends JpaRepository<PollingCenter, UUID> {
}
