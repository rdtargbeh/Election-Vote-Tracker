package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.PollingPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface PollingPlaceRepository extends JpaRepository<PollingPlace, UUID> {

    boolean existsByPollingCenter_CenterIdAndPlaceNumber(UUID centerId, Integer placeNumber);

    Optional<PollingPlace> findByCode(String code);
}
