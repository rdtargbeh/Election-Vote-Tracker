package Backend.ElectionVote.views.repo;


import Backend.ElectionVote.views.entity.CandidateCountyCompare;
import Backend.ElectionVote.views.entity.CandidateCountyCompareId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateCountyCompareRepository extends JpaRepository<CandidateCountyCompare, CandidateCountyCompareId>,
        JpaSpecificationExecutor<CandidateCountyCompare> {
}