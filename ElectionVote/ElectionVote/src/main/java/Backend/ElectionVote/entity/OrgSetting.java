package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;


import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(name = "org_setting")
public class OrgSetting {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "org_id", updatable = false, nullable = false)
    private UUID orgId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "org_id", foreignKey = @ForeignKey(name = "fk_setting_org"))
    private Organization organization;

    /** PostgreSQL JSONB field for flexible key-value settings */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> settings;

}
