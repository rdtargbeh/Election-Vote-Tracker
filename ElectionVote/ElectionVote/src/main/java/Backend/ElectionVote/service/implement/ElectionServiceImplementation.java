package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.ElectionCreateRequest;
import Backend.ElectionVote.dto.ElectionDto;
import Backend.ElectionVote.dto.ElectionSearchRequest;
import Backend.ElectionVote.dto.ElectionUpdateRequest;
import Backend.ElectionVote.entity.Election;
import Backend.ElectionVote.mapper.ElectionMapper;
import Backend.ElectionVote.repository.ElectionRepository;
import Backend.ElectionVote.service.ElectionService;
import Backend.ElectionVote.utility.ElectionSpecs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ElectionServiceImplementation implements ElectionService {

    @PersistenceContext
    EntityManager em;

    private final ElectionRepository electionRepository;
    private final ElectionMapper electionMapper;

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
    public ElectionDto create(ElectionCreateRequest req) {
        /**
         * Creates a new Election entry.
         *
         * @param req contains the election name, year, type, and active status.
         * @return a DTO representing the saved election.
         * @throws ResponseStatusException if an election with the same name and year already exists.
         */
        if (electionRepository.existsByElectionNameIgnoreCaseAndYear(req.getElectionName(), req.getYear())) {
            throw new ResponseStatusException(CONFLICT,
                    "Election '" + req.getElectionName() + "' (" + req.getYear() + ") already exists");
        }

        Election saved = electionRepository.save(electionMapper.toEntity(req));
        return electionMapper.toDTO(saved);
    }


    @Override
    public ElectionDto update(UUID id, ElectionUpdateRequest req) {
        /**
         * Updates an existing Election.
         *
         * @param id  the unique identifier of the election to update.
         * @param req the updated election details.
         * @return a DTO of the updated election.
         * @throws ResponseStatusException if the election is not found or if another election
         *         with the same name and year already exists.
         */
        Election entity = electionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Election not found"));

        boolean dup = electionRepository.existsByElectionNameIgnoreCaseAndYear(req.getElectionName(), req.getYear())
                && !(entity.getElectionName().equalsIgnoreCase(req.getElectionName())
                && entity.getYear() == req.getYear());
        if (dup) {
            throw new ResponseStatusException(CONFLICT,
                    "Another election with name '" + req.getElectionName() + "' and year " + req.getYear() + " exists");
        }

        electionMapper.apply(req, entity);
        return electionMapper.toDTO(electionRepository.save(entity));
    }

    @Override
    public void delete(UUID id) {
        /**
         * Deletes an election by its unique identifier.
         *
         * @param id the unique identifier of the election to delete.
         * @throws ResponseStatusException if the election does not exist.
         */
        if (!electionRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Election not found");
        }
        electionRepository.deleteById(id);
    }


    @Override
    public ElectionDto get(UUID id) {
        /**
         * Retrieves a single election by its unique identifier.
         *
         * @param id the unique identifier of the election.
         * @return a DTO representing the election.
         * @throws ResponseStatusException if the election is not found.
         */
        return electionRepository.findById(id)
                .map(electionMapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Election not found"));
    }


    @Override
    public Page<ElectionDto> search(ElectionSearchRequest req, Pageable pageable) {
        /**
         * Searches for elections based on filters such as name, year, type, and active status.
         *
         * @param req      the search filters encapsulated in an {@link ElectionSearchRequest}.
         * @param pageable pagination and sorting details.
         * @return a paginated list of elections that match the given criteria.
         */
        Specification<Election> spec = Specification
                .where(ElectionSpecs.nameContains(req.q()))
                .and(ElectionSpecs.yearEquals(req.year()))
                .and(ElectionSpecs.typeEquals(req.type()))
                .and(ElectionSpecs.activeEquals(req.active()));

        return electionRepository.findAll(spec, pageable).map(electionMapper::toDTO);
    }


}
