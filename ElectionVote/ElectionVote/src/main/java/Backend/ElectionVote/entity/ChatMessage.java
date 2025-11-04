package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import com.vladmihalcea.hibernate.type.json.JsonType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "chat_message",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_chat_message_sender_client",
                        columnNames = {"sender_id", "client_guid"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "message_id", updatable = false, nullable = false)
    private UUID messageId;

    /** Organization this message belongs to */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_message_org"))
    private Organization organization;

    /** Room where message was sent */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_message_room"))
    private ChatRoom room;

    /** The sender (user who posted the message) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_message_sender"))
    private SystemUser sender;

    /** TEXT, IMAGE, FILE, SYSTEM */
    @Column(name = "content_type", length = 20, nullable = false)
    private String contentType = "TEXT";

    /** Message content — body for TEXT/SYSTEM types */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** JSONB metadata — used for FILE or IMAGE (urls, ids, etc.) */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String metadata = "{}";

    /** Optional reply-to message */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to",
            foreignKey = @ForeignKey(name = "fk_chat_message_reply"))
    private ChatMessage replyTo;

    /** Optional client GUID for idempotent sends */
    @Column(name = "client_guid")
    private UUID clientGuid;

    /** Timestamps */
    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated = LocalDateTime.now();

    @Column(name = "date_edited")
    private LocalDateTime dateEdited;

    @Column(name = "date_deleted")
    private LocalDateTime dateDeleted;
}