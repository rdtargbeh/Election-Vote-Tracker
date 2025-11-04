package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "notification_id", updatable = false, nullable = false)
    private UUID notificationId;

    /** Organization this notification belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_org"))
    private Organization organization;

    /** Target user */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_user"))
    private SystemUser user;

    /** Type category (CHAT, VOTE, SYSTEM, etc.) */
    @Column(name = "type", length = 50, nullable = false)
    private String type;

    /** Short title (e.g., “New Message”, “Vote Approved”) */
    @Column(name = "title", length = 150, nullable = false)
    private String title;

    /** Optional message body */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /** Related table and record (for navigation) */
    @Column(name = "related_table", length = 50)
    private String relatedTable;

    @Column(name = "related_id")
    private UUID relatedId;

    /** Read/Seen flags */
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "is_seen", nullable = false)
    private boolean isSeen = false;

    /** When it was created / read / expires */
    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated = LocalDateTime.now();

    @Column(name = "date_read")
    private LocalDateTime dateRead;

    @Column(name = "date_expires")
    private LocalDateTime dateExpires;

    /** Priority: LOW, NORMAL, HIGH, CRITICAL */
    @Column(name = "priority", length = 20)
    private String priority = "NORMAL";

    /** Delivery channel: IN_APP, EMAIL, SMS, SYSTEM */
    @Column(name = "delivery_method", length = 30)
    private String deliveryMethod = "IN_APP";

    /** Optional creator (for audit trail) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by",
            foreignKey = @ForeignKey(name = "fk_notification_creator"))
    private SystemUser createdBy;
}