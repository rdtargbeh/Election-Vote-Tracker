package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.UserSigningKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSigningKeyRepository extends JpaRepository<UserSigningKey, UUID> {
    List<UserSigningKey> findByUser_UserIdOrderByCreatedAtDesc(UUID userId);
    Optional<UserSigningKey> findByKid(String kid);
}