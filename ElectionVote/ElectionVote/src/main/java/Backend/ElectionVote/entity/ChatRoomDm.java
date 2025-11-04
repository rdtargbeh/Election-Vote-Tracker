package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "chat_room_dm")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDm {

    /** The DM room itself (1:1 with ChatRoom) */
    @Id
    @Column(name = "room_id", nullable = false, updatable = false)
    private UUID roomId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "room_id", foreignKey = @ForeignKey(name = "fk_chat_dm_room"))
    private ChatRoom room;

    /** Organization this DM belongs to */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_dm_org"))
    private Organization organization;

    /** First participant (ordered lower UUID at DB trigger level) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user1_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_dm_user1"))
    private SystemUser user1;

    /** Second participant */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user2_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_dm_user2"))
    private SystemUser user2;
}