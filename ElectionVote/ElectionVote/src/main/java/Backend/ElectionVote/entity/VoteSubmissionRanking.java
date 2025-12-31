package Backend.ElectionVote.entity;

import Backend.ElectionVote.utility.JsonNodeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maps to submission_vote_ranking table.
 *
 * ranking stores a JSON array (JSONB) — each entry should be an option identifier (UUID) in the contest_option table
 * or otherwise an identifier your system understands for ranked ballots.
 *
 * Important assumptions:
 * - The ranking JSON is an array (checked at DB level by CHECK constraint).
 * - The service validates that each array element is a valid UUID and that the option exists for the contestId.
 */
@Entity
@Table(name = "submission_vote_ranking",
        indexes = {
                @Index(name = "idx_svr_submission", columnList = "submission_id"),
                @Index(name = "idx_svr_contest", columnList = "contest_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class VoteSubmissionRanking {

    @Id
    @Column(name = "svr_id", nullable = false)
    private UUID svrId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "contest_id", nullable = false)
    private UUID contestId;

    // Store JSONB via AttributeConverter
    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "ranking", columnDefinition = "jsonb", nullable = false)
    private JsonNode ranking;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @PrePersist
    public void prePersist() {
        if (svrId == null) svrId = UUID.randomUUID();
        if (dateCreated == null) dateCreated = LocalDateTime.now();
    }

}
