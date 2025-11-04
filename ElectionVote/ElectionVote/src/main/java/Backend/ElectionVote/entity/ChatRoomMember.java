package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "chat_room_member",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_chat_room_member_room_user",
                        columnNames = {"room_id", "user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "membership_id", updatable = false, nullable = false)
    private UUID membershipId;

    /** Room this membership belongs to */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_member_room"))
    private ChatRoom room;

    /** Organization guard (must match room.org_id at DB level via trigger) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_member_org"))
    private Organization organization;

    /** The user who is a member of the room */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_member_user"))
    private SystemUser user;

    /** 'MEMBER' or 'ADMIN' */
    @Column(name = "role_name", nullable = false, length = 20)
    private String roleName = "MEMBER";

    /** When the user joined the room */
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    /** Optional pointer to the last message this user has seen */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_seen_message_id",
            foreignKey = @ForeignKey(name = "fk_chat_member_last_seen_msg"))
    private ChatMessage lastSeenMessage;

    /** When the user last viewed the room */
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    /** Whether the room is muted for this user (no notifications) */
    @Column(name = "muted", nullable = false)
    private boolean muted = false;

    /** Membership enabled flag (used by message trigger) */
    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;
}