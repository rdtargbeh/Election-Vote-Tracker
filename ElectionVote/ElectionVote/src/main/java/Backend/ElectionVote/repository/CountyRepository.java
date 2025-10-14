package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.County;
import Backend.ElectionVote.entity.OrgMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CountyRepository extends JpaRepository<County, UUID> {
}
