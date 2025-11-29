package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.AuditLogDto;
import Backend.ElectionVote.entity.AuditLog;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.SystemUser;
import Backend.ElectionVote.enums.ActivityType;
import Backend.ElectionVote.mapper.AuditLogMapper;
import Backend.ElectionVote.repository.AuditLogRepository;
import Backend.ElectionVote.repository.OrganizationRepository;
import Backend.ElectionVote.repository.SystemUserRepository;
import Backend.ElectionVote.service.AuditLogService;
import Backend.ElectionVote.utility.AuditLogSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogServiceImplementation implements AuditLogService {

    private final AuditLogRepository repo;
    private final OrganizationRepository orgRepo;
    private final SystemUserRepository userRepo;
    private final AuditLogMapper mapper = new AuditLogMapper();

    @Override
    public AuditLogDto log(UUID orgId, UUID userId, ActivityType type, String entity, String description) {
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        SystemUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        AuditLog a = new AuditLog();
        a.setOrganization(org);
        a.setUser(user);
        a.setActivityType(type != null ? type : ActivityType.OTHER);
        a.setEntityAffected(entity);
        a.setActionDescription(description);
        // timestamp set by @PrePersist
        return mapper.toDTO(repo.save(a));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> search(UUID orgId, UUID userId, ActivityType type,
                                    LocalDateTime from, LocalDateTime to, String q, Pageable pageable) {
        Specification<AuditLog> spec = Specification
                .where(AuditLogSpecs.orgEquals(orgId))
                .and(AuditLogSpecs.userEquals(userId))
                .and(AuditLogSpecs.typeEquals(type))
                .and(AuditLogSpecs.between(from, to))
                .and(AuditLogSpecs.textSearch(q));

        return repo.findAll(spec, pageable).map(mapper::toDTO);
    }
}