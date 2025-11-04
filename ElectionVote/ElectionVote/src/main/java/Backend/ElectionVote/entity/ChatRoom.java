package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import com.vladmihalcea.hibernate.type.json.JsonType;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "chat_room")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "room_id", updatable = false, nullable = false)
    private UUID roomId;

    /** Organization this chat belongs to */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, foreignKey = @ForeignKey(name = "fk_chat_room_org"))
    private Organization organization;

    /** GROUP / CHANNEL / DM */
    @Column(name = "room_type", nullable = false, length = 20)
    private String roomType;

    /** Display name (null for DM) */
    @Column(length = 150)
    private String name;

    /** Optional description */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** User who created the room */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, foreignKey = @ForeignKey(name = "fk_chat_room_user"))
    private SystemUser createdBy;

    /** Creation timestamp */
    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated = LocalDateTime.now();

    /** Whether the room is archived */
    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;

    /** JSONB settings: e.g., {"slowmode_sec": 3} */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String settings = "{}";
}
