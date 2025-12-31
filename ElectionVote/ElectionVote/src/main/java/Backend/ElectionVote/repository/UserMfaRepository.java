package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.UserMfa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserMfaRepository extends JpaRepository<UserMfa, UUID> {
    Optional<UserMfa> findByUser_UserId(UUID userId);
}