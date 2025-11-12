package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.TallySheetCreateByUrlRequest;
import Backend.ElectionVote.dto.TallySheetDto;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.TallySheet;
import Backend.ElectionVote.entity.VoteSubmission;
import Backend.ElectionVote.mapper.TallySheetMapper;
import Backend.ElectionVote.repository.OrganizationRepository;
import Backend.ElectionVote.repository.TallySheetRepository;
import Backend.ElectionVote.repository.VoteSubmissionRepository;
import Backend.ElectionVote.service.FileStorageService;
import Backend.ElectionVote.service.TallySheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class TallySheetServiceImplementation implements TallySheetService {

    private final TallySheetRepository repo;
    private final OrganizationRepository orgRepo;
    private final VoteSubmissionRepository submissionRepo;
    @Autowired
    private FileStorageService storage;
    private final TallySheetMapper mapper = new TallySheetMapper();

    @Override
    @Transactional
    public TallySheetDto upload(UUID orgId, UUID submissionId, MultipartFile file) {
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        VoteSubmission sub = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        // org consistency will also be enforced by DB trigger, but fail fast here:
        if (!sub.getOrganization().getOrgId().equals(org.getOrgId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission belongs to a different organization");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String sha = sha256(file);
        // Optional: avoid duplicates at submission level
        if (sha != null && repo.existsBySubmissionAndSha(submissionId, sha)) {
            // return existing or reject; here we reject:
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate file (same SHA-256) for this submission");
        }

        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "upload.bin");
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')+1) : "bin";
        String storedName = sub.getSubmissionId() + "-" + UUID.randomUUID() + "." + ext;

        String url;
        try (InputStream in = file.getInputStream()) {
            url = storage.store("tally-sheets", storedName, in, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }

        TallySheet t = new TallySheet();
        t.setOrganization(org);
        t.setSubmission(sub);
        t.setImageUrl(url);
        t.setFileSha256(sha);
        // dateUploaded & lastUpdated are handled by @PrePersist/@PreUpdate in the entity

        TallySheet saved = repo.save(t);
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public TallySheetDto createByUrl(TallySheetCreateByUrlRequest req) {
        Organization org = orgRepo.findById(req.getOrgId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        VoteSubmission sub = submissionRepo.findById(req.getSubmissionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        if (!sub.getOrganization().getOrgId().equals(org.getOrgId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission belongs to a different organization");
        }
        if (req.getImageUrl() == null || req.getImageUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageUrl is required");
        }
        if (req.getFileSha256() != null && repo.existsBySubmissionAndSha(req.getSubmissionId(), req.getFileSha256())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate file (same SHA-256) for this submission");
        }

        TallySheet t = new TallySheet();
        t.setOrganization(org);
        t.setSubmission(sub);
        t.setImageUrl(req.getImageUrl());
        t.setFileSha256(req.getFileSha256());
        t.setOcrExtracted(req.getOcrExtracted());

        return mapper.toDTO(repo.save(t));
    }

    @Override
    @Transactional
    public TallySheetDto setOcr(UUID uploadId, String ocrJson) {
        TallySheet t = repo.findById(uploadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tally sheet not found"));
        t.setOcrExtracted(ocrJson);
        return mapper.toDTO(repo.save(t));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TallySheetDto> listBySubmission(UUID submissionId) {
        return repo.findBySubmission_SubmissionIdOrderByDateUploadedDesc(submissionId)
                .stream().map(mapper::toDTO).toList();
    }

    @Override
    @Transactional
    public void delete(UUID uploadId) {
        if (!repo.existsById(uploadId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tally sheet not found");
        }
        repo.deleteById(uploadId);
    }

    @Override
    @Transactional(readOnly = true)
    public TallySheetDto get(UUID uploadId) {
        return repo.findById(uploadId)
                .map(mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tally sheet not found"));
    }

    // ---------- helpers ----------
    private static String sha256(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = file.getInputStream()) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) md.update(buf, 0, r);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null; // don’t fail upload if hashing fails
        }
    }
}
