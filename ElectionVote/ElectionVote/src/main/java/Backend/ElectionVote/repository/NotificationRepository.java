package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);
}