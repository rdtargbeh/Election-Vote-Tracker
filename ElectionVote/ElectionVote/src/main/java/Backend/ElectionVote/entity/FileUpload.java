package Backend.ElectionVote.entity;

import Backend.ElectionVote.enums.FileType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(name = "file_upload")
public class FileUpload {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "file_id", nullable = false, updatable = false)
    private UUID fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false,
            foreignKey = @ForeignKey(name = "file_upload_org_id_fkey"))
    private Organization organization;

    /** Polymorphic link to parent row (vote_submission, observer_report, etc.) */
    @Column(name = "related_table", length = 50, nullable = false)
    private String relatedTable;

    @Column(name = "related_id", nullable = false)
    private UUID relatedId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 50, nullable = false)
    private FileType fileType;

    @Column(name = "file_url", nullable = false, columnDefinition = "text")
    private String fileUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false,
            foreignKey = @ForeignKey(name = "file_upload_uploaded_by_fkey"))
    private SystemUser uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @PrePersist
    void prePersist() {
        if (uploadedAt == null) uploadedAt = LocalDateTime.now();
    }
}
