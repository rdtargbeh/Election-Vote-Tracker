package Backend.ElectionVote.entity;

import Backend.ElectionVote.enums.FileType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
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
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "file_id", updatable = false, nullable = false)
    private UUID fileId;

    /** Organization that owns the file */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "org_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_file_upload_org")
    )
    private Organization organization;

    /** Related table name (e.g., 'chat_message', 'vote_submission') */
    @Column(name = "related_table", nullable = false, length = 50)
    private String relatedTable;

    /** Related record ID in the related table */
    @Column(name = "related_id", nullable = false)
    private UUID relatedId;

    /** File type classification */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 50, nullable = false)
    private FileType fileType;

    /** Public or internal file URL (S3, GCS, etc.) */
    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    /** MIME type (e.g., image/jpeg) */
    @Column(name = "mime_type", length = 150)
    private String mimeType;

    /** File size in bytes */
    @Column(name = "size_bytes")
    private Long sizeBytes;

    /** Optional SHA-256 checksum */
    @Column(name = "sha256", columnDefinition = "TEXT")
    private String sha256;

    /** Storage provider (S3, GCS, AZURE, LOCAL) */
    @Column(name = "storage_provider", length = 30)
    private String storageProvider;

    /** The user who uploaded the file */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "uploaded_by",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_file_upload_user")
    )
    private SystemUser uploadedBy;

    /** JSONB field for additional metadata/tags */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String tags = "{}";

    /** Soft delete timestamp */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Last update timestamp */
    @Column(name = "date_updated")
    private LocalDateTime dateUpdated = LocalDateTime.now();
}
