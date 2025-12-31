package Backend.ElectionVote.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA mapping for contest table.
 */
@Entity
@Table(name = "contest")
@Getter
@Setter
@NoArgsConstructor
public class Contest {

    @Id
    @Column(name = "contest_id", nullable = false)
    private UUID contestId;

    @Column(name = "election_id", nullable = false)
    private UUID electionId;

    @Column(name = "contest_name", nullable = false)
    private String contestName;

    @Column(name = "contest_type")
    private String contestType; // e.g. SINGLE_CHOICE, MULTI_CHOICE, RANKED

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @PrePersist
    public void prePersist() {
        if (contestId == null) contestId = UUID.randomUUID();
        if (dateCreated == null) dateCreated = LocalDateTime.now();
        if (isActive == null) isActive = Boolean.TRUE;
        if (contestType == null) contestType = "SINGLE_CHOICE";
    }
}