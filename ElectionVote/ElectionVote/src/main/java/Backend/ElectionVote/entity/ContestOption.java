package Backend.ElectionVote.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Minimal mapping for contest_option table used when normalizing submissions.
 * Fields chosen to match SQL usage in submission normalization:
 *  - optionId  -> option_id (PK)
 *  - contestId -> contest_id
 *  - candidateId -> candidate_id
 *
 * Keep this lightweight (no relationships) so normalization logic can resolve candidate -> option.
 */
@Entity
@Table(name = "contest_option",
        indexes = {
                @Index(name = "idx_contest_option_candidate", columnList = "candidate_id"),
                @Index(name = "idx_contest_option_contest", columnList = "contest_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class ContestOption {

    @Id
    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    // keep explicit contestId column to allow simple writes; relation below is read-only convenience
    @Column(name = "contest_id", nullable = false)
    private UUID contestId;

    // optional candidate reference (UUID). If you have a Candidate entity and want a relation, add it separately.
    @Column(name = "candidate_id")
    private UUID candidateId;

    @Column(name = "option_label")
    private String optionLabel;

    @Column(name = "option_order")
    private Integer optionOrder;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    // convenience relationship to Contest (read-only via insertable=false, updatable=false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", insertable = false, updatable = false)
    private Contest contest;

    @PrePersist
    public void prePersist() {
        if (optionId == null) optionId = UUID.randomUUID();
        if (dateCreated == null) dateCreated = LocalDateTime.now();
        if (isActive == null) isActive = Boolean.TRUE;
    }
}