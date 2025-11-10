package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.Election;
import Backend.ElectionVote.enums.ElectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectionRepository extends JpaRepository<Election, UUID>, JpaSpecificationExecutor<Election> {

    boolean existsByElectionNameIgnoreCaseAndYear(String electionName, int year);


    Optional<Election> findFirstByIsActiveTrueAndElectionType(ElectionType type);
}