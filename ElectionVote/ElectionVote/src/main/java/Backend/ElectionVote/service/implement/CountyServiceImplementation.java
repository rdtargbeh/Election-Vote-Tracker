package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.CountyDto;
import Backend.ElectionVote.dto.CountyRequest;
import Backend.ElectionVote.entity.County;
import Backend.ElectionVote.mapper.CountyMapper;
import Backend.ElectionVote.repository.CountyRepository;
import Backend.ElectionVote.service.CountyService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CountyServiceImplementation implements CountyService {

    @Autowired
    private CountyRepository countyRepository;
    @Autowired
    private CountyMapper countyMapper = new CountyMapper();


    @Override
    public CountyDto create(CountyRequest request) {
        if (countyRepository.existsByCountyNameIgnoreCase(request.countyName()))
            throw new EntityExistsException("County already exists: " + request.countyName());

        County county = countyMapper.toEntity(request);
        county = countyRepository.save(county);
        return countyMapper.toDTO(county);
    }

    @Override
    public CountyDto update(UUID id, CountyRequest request) {
        County county = countyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("County not found"));
        countyMapper.updateEntity(county, request);
        return countyMapper.toDTO(countyRepository.save(county));
    }


    @Override
    public void delete(UUID countyId) {
        if (!countyRepository.existsById(countyId)) throw new EntityNotFoundException("County not found");
        countyRepository.deleteById(countyId);
    }

    @Override
    @Transactional(readOnly = true)
    public CountyDto get(UUID countyId) {
        County entity = countyRepository.findById(countyId)
                .orElseThrow(() -> new EntityNotFoundException("County not found"));
        return countyMapper.toDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CountyDto> list(String q, Pageable pageable) {
        Page<County> page = (q == null || q.isBlank())
                ? countyRepository.findAll(pageable)
                : countyRepository.findByCountyNameContainingIgnoreCase(q.trim(), pageable);
        return page.map(countyMapper::toDTO);
    }

    private String validateAndNormalize(String raw) {
        if (raw == null) throw new IllegalArgumentException("countyName is required");
        String name = raw.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("countyName cannot be blank");
        if (name.length() > 100) throw new IllegalArgumentException("countyName max length is 100");
        return name;
    }

    private void guardUnique(String name, UUID selfId) {
        boolean exists = countyRepository.existsByCountyNameIgnoreCase(name);
        if (!exists) return;
        if (selfId != null) {
            County current = countyRepository.findById(selfId).orElse(null);
            if (current != null && current.getCountyName().equalsIgnoreCase(name)) return;
        }
        throw new EntityExistsException("County name already exists: " + name);
    }


  }