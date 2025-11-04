package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.ChatRoomMember;
import Backend.ElectionVote.entity.SystemUser;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {

    @Query("""
           select m.user
           from ChatRoomMember m
           where m.room.roomId = :roomId
             and m.isEnabled = true
             and m.muted = false
             and m.user.userId <> :senderUserId
           """)
    List<SystemUser> findActiveUsersToNotify(UUID roomId, UUID senderUserId);
}