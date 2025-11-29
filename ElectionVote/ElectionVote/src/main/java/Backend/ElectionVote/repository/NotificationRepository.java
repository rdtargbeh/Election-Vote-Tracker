package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {

    List<Notification> findByUser_UserIdAndIsReadFalseOrderByDateCreatedDesc(UUID userId);

    @Query("select count(n) from Notification n where n.user.userId = :userId and n.isRead = false")
    long countUnread(@Param("userId") UUID userId);


    @Modifying
    @Query("update Notification n set n.isRead = true, n.dateRead = CURRENT_TIMESTAMP " +
            "where n.notificationId in :ids and n.user.userId = :userId and n.isRead = false")
    int markRead(@Param("userId") UUID userId, @Param("ids") List<UUID> ids);

    @Modifying
    @Query("update Notification n set n.isSeen = true " +
            "where n.notificationId in :ids and n.user.userId = :userId and n.isSeen = false")
    int markSeen(@Param("userId") UUID userId, @Param("ids") List<UUID> ids);

    Optional<Notification> findByIdempotencyKey(String key);



}