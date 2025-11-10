package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.ChatRoomMember;
import Backend.ElectionVote.entity.SystemUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {

    List<ChatRoomMember>
    findAllByRoom_RoomIdAndIsEnabledTrueAndMutedFalseAndUser_UserIdNot(
            UUID roomId, UUID senderUserId);



//    @Query("""
//         select distinct m.user
//         from ChatRoomMember m
//         where m.room.roomId = :roomId
//           and m.isEnabled = true
//           and m.muted = false
//           and m.user.userId <> :senderUserId
//         """)
//    List<SystemUser> findActiveUsersToNotify(@Param("roomId") UUID roomId,
//                                             @Param("senderUserId") UUID senderUserId);


}