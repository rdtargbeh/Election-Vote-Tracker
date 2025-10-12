package Backend.ElectionVote.entity;

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
@Table(name = "tally_sheet")
public class TallySheet {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "upload_id", nullable = false, updatable = false)
    private UUID uploadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false,
            foreignKey = @ForeignKey(name = "tally_sheet_org_id_fkey"))
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false,
            foreignKey = @ForeignKey(name = "tally_sheet_submission_id_fkey"))
    private VoteSubmission submission;

    @Column(name = "image_url", nullable = false, columnDefinition = "text")
    private String imageUrl;

    @Column(name = "file_sha256", columnDefinition = "text")
    private String fileSha256;

    @Column(name = "date_uploaded")
    private LocalDateTime dateUploaded;

    // Store JSONB as String for now (can switch to Map<String,Object> later)
    @Column(name = "ocr_extracted", columnDefinition = "jsonb")
    private String ocrExtracted;

    @PrePersist
    void prePersist() {
        if (dateUploaded == null) dateUploaded = LocalDateTime.now();
    }
}
