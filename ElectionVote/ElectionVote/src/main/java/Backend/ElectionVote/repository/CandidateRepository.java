package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    boolean existsByFullNameIgnoreCase(String fullName);

}
