package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.ElectionCreateRequest;
import Backend.ElectionVote.dto.ElectionDto;
import Backend.ElectionVote.dto.ElectionSearchRequest;
import Backend.ElectionVote.dto.ElectionUpdateRequest;
import Backend.ElectionVote.entity.Election;
import Backend.ElectionVote.mapper.ElectionMapper;
import Backend.ElectionVote.repository.ElectionRepository;
import Backend.ElectionVote.repository.StatsRepository;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.security.CurrentUserProvider;
import Backend.ElectionVote.service.ElectionService;
import Backend.ElectionVote.service.ElectionStatsProjection;
import Backend.ElectionVote.utility.ElectionSpecs;
import Backend.ElectionVote.utility.ElectionStatsDto;
import Backend.ElectionVote.utility.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ElectionServiceImplementation implements ElectionService {

    @PersistenceContext
    EntityManager em;

    private final ElectionRepository electionRepository;
    private final StatsRepository statsRepository;
    private final ElectionMapper electionMapper;
    private final AuthorizationService authz;
    private final CurrentUserProvider currentUserProvider;
    private final JdbcTemplate jdbc;

    /**
     * Handles all core business operations for the Election entity.
     * Provides methods to create, update, delete, retrieve, and search elections.
     *
     * <p>This service ensures that:
     * <ul>
     *   <li>Each election name and year combination is unique.</li>
     *   <li>Appropriate HTTP status codes are returned when resources are missing or duplicate.</li>
     *   <li>All entity–DTO transformations are handled through {@link electionMapper}.</li>
     * </ul>
     * </p>
     */
    @Override
    @Transactional
    public ElectionDto create(ElectionCreateRequest req) {
        if (electionRepository.existsByElectionNameIgnoreCaseAndYear(
                req.getElectionName(), req.getYear())) {
            throw new ResponseStatusException(CONFLICT, "Election '" + req.getElectionName() + "' (" + req.getYear() + ") already exists"
            );
        }
        Election saved = electionRepository.save(electionMapper.toEntity(req));

        // Audit log (best-effort)
        try {
            // Resolve actor user id in a type-safe way
            UUID actor = null;
            // 1) prefer currentUserProvider if available
            try {
                actor = (currentUserProvider != null ? currentUserProvider.currentUserId() : null);
            } catch (Exception ignored) {}
            // 2) fallback to TenantContext if still null
            if (actor == null) {
                try {
                    var ctx = TenantContext.get();
                    if (ctx != null) actor = ctx.userId().orElse(null);
                } catch (Exception ignored) {}
            }
            String desc = "Created election: " + saved.getElectionName();
            // Insert simple audit_log entry (org_id null for global election)
            jdbc.update(
                    "INSERT INTO audit_log (log_id, org_id, user_id, activity_type, entity_affected, action_description) " +
                            "VALUES (gen_random_uuid(), ?, ?, ?, ?, ?)",
                    new Object[]{ null, actor, "ELECTION_CREATE", "election", desc }
            );
        } catch (Exception ignored) {}


        return electionMapper.toDTO(saved);
    }


    @Override
    @Transactional
    public ElectionDto update(UUID id, ElectionUpdateRequest req) {
        Election entity = electionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Election not found"));

        boolean dup = electionRepository
                .existsByElectionNameIgnoreCaseAndYear(req.getElectionName(), req.getYear())
                && !(entity.getElectionName().equalsIgnoreCase(req.getElectionName())
                && entity.getYear() == req.getYear());

        if (dup) {
            throw new ResponseStatusException(CONFLICT, "Another election with name '" + req.getElectionName()
                            + "' and year " + req.getYear() + " exists"
            );
        }

        electionMapper.apply(req, entity);
        Election saved = electionRepository.save(entity);

        // Audit log (best-effort)
        try {
            UUID actor = null;
            jdbc.update(
                    "INSERT INTO audit_log (log_id, org_id, user_id, activity_type, entity_affected, action_description) " +
                            "VALUES (gen_random_uuid(), NULL, ?, ?, ?, ?)",
                    new Object[]{ actor, "ELECTION_UPDATE", "election", "Updated election " + saved.getElectionName() }
            );
        } catch (Exception ignored) {}

        return electionMapper.toDTO(saved);
    }


    /**
     * Deletes an election by its unique identifier.
     *
     * @param id the unique identifier of the election to delete.
     * @throws ResponseStatusException if the election does not exist.
     */
    @Override
    @Transactional
    public void delete(UUID id) {

        if (!electionRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Election not found");
        }

        // Prevent deletion when official NEC results exist for this election
        Number cnt = (Number) em.createNativeQuery("select count(*) from nec_result where election_id = ?")
                .setParameter(1, id.toString())
                .getSingleResult();

        if (cnt != null && cnt.longValue() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete election with official results");
        }

        electionRepository.deleteById(id);

        // Audit log (best-effort)
        try {
            UUID actor = null;
            jdbc.update(
                    "INSERT INTO audit_log (log_id, org_id, user_id, activity_type, entity_affected, action_description) " +
                            "VALUES (gen_random_uuid(), NULL, ?, ?, ?, ?)",
                    new Object[]{ actor, "ELECTION_DELETE", "election", "Deleted election " + id }
            );
        } catch (Exception ignored) {}

    }


    /**
     * Retrieves a single election by its unique identifier.
     *
     * @param id the unique identifier of the election.
     * @return a DTO representing the election.
     * @throws ResponseStatusException if the election is not found.
     */
    @Override
    public ElectionDto get(UUID id) {

        return electionRepository.findById(id)
                .map(electionMapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Election not found"));
    }


    @Override
    public List<ElectionDto> listActiveElections() {
        List<Election> elections = electionRepository.findByIsActiveTrueOrderByDateCreatedDesc();
        return elections.stream()
                .map(electionMapper::toDTO)
                .toList();
    }

    @Override
    public List<ElectionDto> listAllElections() {
        return electionRepository.findAll().stream()
                .map(electionMapper::toDTO)
                .toList();
    }

    /**
     * Searches for elections based on filters such as name, year, type, and active status.
     *
     * @param req      the search filters encapsulated in an {@link ElectionSearchRequest}.
     * @param pageable pagination and sorting details.
     * @return a paginated list of elections that match the given criteria.
     */
    @Override
    public Page<ElectionDto> search(ElectionSearchRequest req, Pageable pageable) {
        Specification<Election> spec = Specification
                .where(ElectionSpecs.nameContains(req.q()))
                .and(ElectionSpecs.yearEquals(req.year()))
                .and(ElectionSpecs.typeEquals(req.type()))
                .and(ElectionSpecs.activeEquals(req.active()));

        return electionRepository.findAll(spec, pageable)
                .map(electionMapper::toDTO);
    }



    @Override
    public ElectionStatsDto getOrgElectionStats(UUID orgId, UUID electionId) {
        ElectionStatsProjection p = statsRepository
                .findOrgElectionStats(orgId, electionId)
                .orElse(null);

        if (p == null) {
            // No row yet → return empty stats instead of error
            return ElectionStatsDto.builder()
                    .orgId(orgId)
                    .electionId(electionId)
                    .registeredVoters(0)
                    .ballotsCast(0)
                    .validVotes(0)
                    .invalidTotal(0)
                    .turnoutPct(BigDecimal.ZERO)
                    .invalidPct(BigDecimal.ZERO)
                    .build();
        }

        return ElectionStatsDto.builder()
                .orgId(p.getOrgId())
                .electionId(p.getElectionId())
                .registeredVoters(p.getRegisteredVoters() != null ? p.getRegisteredVoters() : 0)
                .ballotsCast(p.getBallotsCast() != null ? p.getBallotsCast() : 0)
                .validVotes(p.getValidVotes() != null ? p.getValidVotes() : 0)
                .invalidTotal(p.getInvalidTotal() != null ? p.getInvalidTotal() : 0)
                .turnoutPct(p.getTurnoutPct() != null ? p.getTurnoutPct() : BigDecimal.ZERO)
                .invalidPct(p.getInvalidPct() != null ? p.getInvalidPct() : BigDecimal.ZERO)
                .build();
    }





}
