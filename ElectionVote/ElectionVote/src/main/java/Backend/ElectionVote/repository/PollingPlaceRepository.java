package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.PollingPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface PollingPlaceRepository
        extends JpaRepository<PollingPlace, UUID>, JpaSpecificationExecutor<PollingPlace> {

        boolean existsByPollingCenter_CenterIdAndPlaceNumber(UUID centerId, Integer placeNumber);

    Optional<PollingPlace> findByCode(String code);

    List<PollingPlace> findByPollingCenter_CenterIdOrderByPlaceNumberAsc(UUID centerId);


}
