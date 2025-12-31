package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.Contest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContestRepository extends JpaRepository<Contest, UUID> {
    List<Contest> findByElectionId(UUID electionId);
    List<Contest> findByElectionIdAndIsActiveTrue(UUID electionId);
}