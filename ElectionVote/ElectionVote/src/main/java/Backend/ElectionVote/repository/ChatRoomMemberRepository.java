package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.ChatRoomMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {

    List<ChatRoomMember> findAllByRoom_RoomIdAndIsEnabledTrueAndMutedFalseAndUser_UserIdNot(
            UUID roomId, UUID senderUserId);

    Optional<ChatRoomMember> findByRoom_RoomIdAndUser_UserId(UUID roomId, UUID userId);

    Page<ChatRoomMember> findByRoom_RoomId(UUID roomId, Pageable pageable);

    boolean existsByRoom_RoomIdAndUser_UserId(UUID roomId, UUID userId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("""
        update ChatRoomMember m
        set m.lastSeenAt = :ts
        where m.room.roomId = :roomId and m.user.userId = :userId
    """)
    int updateLastSeenAt(@org.springframework.data.repository.query.Param("roomId") UUID roomId,
                         @org.springframework.data.repository.query.Param("userId") UUID userId,
                         @org.springframework.data.repository.query.Param("ts") java.time.LocalDateTime ts);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        update ChatRoomMember m
        set m.lastPostAt = :ts
        where m.room.roomId = :roomId and m.user.userId = :userId
    """)
    int bumpByLastPostAt(@org.springframework.data.repository.query.Param("roomId") UUID roomId,
                         @org.springframework.data.repository.query.Param("userId") UUID userId,
                         @org.springframework.data.repository.query.Param("ts") java.time.LocalDateTime ts);



}