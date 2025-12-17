package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.ContestOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Minimal repository used to resolve candidate -> contest option mapping.
 * Assumes ContestOption entity exists and has candidateId property.
 */

@Repository
public interface ContestOptionRepository extends JpaRepository<ContestOption, UUID> {
    List<ContestOption> findByContestIdOrderByOptionOrderAsc(UUID contestId);
    List<ContestOption> findByContestIdAndIsActiveTrueOrderByOptionOrderAsc(UUID contestId);
    List<ContestOption> findByCandidateId(UUID candidateId);

    @Query("select coalesce(max(c.optionOrder), 0) from ContestOption c where c.contestId = ?1")
    Integer findMaxOptionOrderForContest(UUID contestId);
}