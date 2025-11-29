package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.ChatRoomDm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

import java.util.UUID;

public interface ChatRoomDmRepository extends JpaRepository<ChatRoomDm, UUID> {

    // Exact pair (user1,user2) — call with canonical order from service
    Optional<ChatRoomDm> findByOrganization_OrgIdAndUser1_UserIdAndUser2_UserId(
            UUID orgId, UUID user1Id, UUID user2Id
    );

    // List my DMs (two sides, merge in service)
    List<ChatRoomDm> findByOrganization_OrgIdAndUser1_UserId(UUID orgId, UUID userId);

    List<ChatRoomDm> findByOrganization_OrgIdAndUser2_UserId(UUID orgId, UUID userId);
}
