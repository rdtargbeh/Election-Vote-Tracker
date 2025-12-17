package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.OrgSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrgSettingRepository extends JpaRepository<OrgSetting, UUID> {
    Optional<OrgSetting> findByOrganization_OrgId(UUID orgId);
}
