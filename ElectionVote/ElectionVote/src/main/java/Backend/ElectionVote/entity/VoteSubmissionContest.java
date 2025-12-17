package Backend.ElectionVote.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maps to submission_contest_vote table.
 */
@Entity
@Table(name = "submission_contest_vote",
        indexes = {
                @Index(name = "idx_scv_submission", columnList = "submission_id"),
                @Index(name = "idx_scv_org_elec_contest", columnList = "org_id, election_id, contest_id"),
                @Index(name = "idx_scv_contest_option", columnList = "contest_id, option_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class VoteSubmissionContest {

    @Id
    @Column(name = "scv_id", nullable = false)
    private UUID scvId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "election_id", nullable = false)
    private UUID electionId;

    @Column(name = "contest_id", nullable = false)
    private UUID contestId;

    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    @Column(name = "vote_value", nullable = false)
    private Integer voteValue = 1;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @PrePersist
    public void prePersist() {
        if (scvId == null) scvId = UUID.randomUUID();
        if (dateCreated == null) dateCreated = LocalDateTime.now();
        if (voteValue == null) voteValue = 1;
    }
}