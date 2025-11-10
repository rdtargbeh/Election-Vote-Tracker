package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.ElectionCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ElectionCandidateRepository extends JpaRepository<ElectionCandidate, UUID> {

    boolean existsByElection_ElectionIdAndCandidate_CandidateId(UUID electionId, UUID candidateId);
}