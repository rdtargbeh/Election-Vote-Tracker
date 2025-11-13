package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.FileUploadCreateRequest;
import Backend.ElectionVote.dto.FileUploadDto;
import Backend.ElectionVote.entity.*;
import Backend.ElectionVote.enums.FileType;
import Backend.ElectionVote.enums.StorageProvider;
import Backend.ElectionVote.mapper.FileUploadMapper;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.service.FileStorageService;
import Backend.ElectionVote.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;


@Service
@RequiredArgsConstructor
public class FileUploadServiceImplementation implements FileUploadService {

    private final FileUploadRepository fileUploadRepository;
    private final OrganizationRepository orgRepo;
    private final SystemUserRepository userRepo;
    private final FileStorageService storage;

    // Optionally wire these to auto-create companion records:
    private final TallySheetRepository tallyRepo;              // optional
    private final VoteSubmissionRepository submissionRepo;     // for guard when related_table=vote_submission
    private final ObserverReportRepository observerRepo;       // guard when related_table=observer_report
    private final ChatMessageRepository chatRepo;              // guard when related_table=chat_message

    private final FileUploadMapper mapper = new FileUploadMapper();


    @Override
    @Transactional
    public FileUploadDto uploadMultipart(FileUploadCreateRequest meta, MultipartFile file, UUID uploadedBy) {
        Organization org = orgRepo.findById(meta.getOrgId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        SystemUser user = userRepo.findById(uploadedBy)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        requireRelatedExistsAndSameOrg(meta.getRelatedTable(), meta.getRelatedId(), org.getOrgId());

        if (file == null || file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");

        String sha = safeSha256(file);

        // org-scoped de-dup (matches SQL unique index uq_file_by_org_sha on non-deleted)
        if (sha != null && fileUploadRepository.existsActiveByOrgAndSha(org.getOrgId(), sha)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate file for this organization (same SHA-256)");
        }

        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "upload.bin");
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1) : "bin";
        String storedName = meta.getRelatedId() + "-" + UUID.randomUUID() + "." + ext;

        String url;
        try (InputStream in = file.getInputStream()) {
            url = storage.store(meta.getRelatedTable(), storedName, in, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }

        FileUpload f = new FileUpload();
        f.setOrganization(org);
        f.setRelatedTable(meta.getRelatedTable());
        f.setRelatedId(meta.getRelatedId());
        f.setFileType(meta.getFileType());
        f.setFileUrl(url);
        f.setMimeType(meta.getMimeType() != null ? meta.getMimeType() : file.getContentType());
        f.setSizeBytes(meta.getSizeBytes() != null ? meta.getSizeBytes() : file.getSize());
        f.setSha256(sha);
        f.setStorageProvider(meta.getStorageProvider());
        f.setUploadedBy(user);
        f.setTags(meta.getTags() != null ? new HashMap<>(meta.getTags()) : new HashMap<>());

        FileUpload saved = persistWithConflictHandling(f);

        // Optional: if this is a TALLY_SHEET for a vote_submission, also persist a TallySheet row
        maybeMirrorToTallySheet(saved);

        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public FileUploadDto createByUrl(FileUploadCreateRequest req, UUID uploadedBy) {
        Organization org = orgRepo.findById(req.getOrgId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        SystemUser user = userRepo.findById(uploadedBy)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        requireRelatedExistsAndSameOrg(req.getRelatedTable(), req.getRelatedId(), org.getOrgId());

        if (req.getFileUrl() == null || req.getFileUrl().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileUrl is required");

        if (req.getSha256() != null && fileUploadRepository.existsActiveByOrgAndSha(org.getOrgId(), req.getSha256())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate file for this organization (same SHA-256)");
        }

        FileUpload f = new FileUpload();
        f.setOrganization(org);
        f.setRelatedTable(req.getRelatedTable());
        f.setRelatedId(req.getRelatedId());
        f.setFileType(req.getFileType());
        f.setFileUrl(req.getFileUrl());
        f.setMimeType(req.getMimeType());
        f.setSizeBytes(req.getSizeBytes());
        f.setSha256(req.getSha256());
        f.setStorageProvider(req.getStorageProvider());
        f.setUploadedBy(user);
        f.setTags(req.getTags() != null ? new HashMap<>(req.getTags()) : new HashMap<>());

        FileUpload saved = persistWithConflictHandling(f);
        maybeMirrorToTallySheet(saved);
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileUploadDto> list(UUID orgId, String relatedTable, UUID relatedId) {
        return fileUploadRepository.listActive(orgId, relatedTable, relatedId).stream().map(mapper::toDTO).toList();
    }

    @Override
    @Transactional
    public void softDelete(UUID fileId, UUID requesterId) {
        FileUpload f = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        // (Optional) check requester permissions here
        if (f.isDeleted()) return;
        f.softDelete();
        fileUploadRepository.save(f);
    }

    @Override
    @Transactional(readOnly = true)
    public FileUploadDto get(UUID fileId) {
        return fileUploadRepository.findById(fileId).map(mapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    // ---------- helpers ----------

    private void requireRelatedExistsAndSameOrg(String table, UUID relatedId, UUID orgId) {
        switch (table) {
            case "vote_submission" -> {
                var sub = submissionRepo.findById(relatedId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "vote_submission not found"));
                if (!sub.getOrganization().getOrgId().equals(orgId))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Related vote_submission belongs to another org");
            }
            case "tally_sheet" -> {
                var ts = tallyRepo.findById(relatedId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "tally_sheet not found"));
                if (!ts.getOrganization().getOrgId().equals(orgId))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Related tally_sheet belongs to another org");
            }
            case "observer_report" -> {
                var r = observerRepo.findById(relatedId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "observer_report not found"));
                if (!r.getOrganization().getOrgId().equals(orgId))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Related observer_report belongs to another org");
            }
            case "chat_message" -> {
                var m = chatRepo.findById(relatedId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "chat_message not found"));
                if (!m.getOrganization().getOrgId().equals(orgId))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Related chat_message belongs to another org");
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported related_table: " + table);
        }
        // DB triggers (trg_fu_guard_dispatch) will double-check this too.
    }

    private static String safeSha256(MultipartFile file) {
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
            return null;
        }
    }

    private FileUpload persistWithConflictHandling(FileUpload f) {
        try {
            return fileUploadRepository.save(f);
        } catch (DataIntegrityViolationException ex) {
            // covers unique index uq_file_by_org_sha (non-deleted)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate file for this organization", ex);
        }
    }

    /** If the upload corresponds to a tally sheet for a submission, mirror it into the tally_sheet table. */
    private void maybeMirrorToTallySheet(FileUpload saved) {
        if (saved.getFileType() == FileType.TALLY_SHEET && "vote_submission".equals(saved.getRelatedTable())) {
            // create a simple TallySheet row; org consistency is enforced by DB trigger as well
            VoteSubmission sub = submissionRepo.findById(saved.getRelatedId())
                    .orElse(null);
            if (sub != null && sub.getOrganization().getOrgId().equals(saved.getOrganization().getOrgId())) {
                TallySheet t = new TallySheet();
                t.setOrganization(saved.getOrganization());
                t.setSubmission(sub);
                t.setImageUrl(saved.getFileUrl());
                t.setFileSha256(saved.getSha256());
                tallyRepo.save(t);
            }
        }
    }


    @Override
    public List<FileUploadDto> saveAllForEntity(
            Organization org,
            String relatedTable,
            UUID relatedId,
            SystemUser uploadedBy,
            List<MultipartFile> files,
            Map<String, Object> tags
    ) {
        if (org == null || org.getOrgId() == null) {
            throw new IllegalArgumentException("Organization is required");
        }
        if (relatedTable == null || relatedTable.isBlank()) {
            throw new IllegalArgumentException("relatedTable is required");
        }
        if (relatedId == null) {
            throw new IllegalArgumentException("relatedId is required");
        }
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        final String folder = buildFolder(relatedTable, relatedId); // e.g. observer_report/{UUID}/2025-11-12
        final Map<String, Object> baseTags = tags != null ? new HashMap<>(tags) : new HashMap<>();

        List<FileUploadDto> out = new ArrayList<>(files.size());
        for (MultipartFile mf : files) {
            if (mf.isEmpty()) continue;

            try (InputStream in = mf.getInputStream()) {

                // 1) Compute SHA-256 for de-dup (per org)
                String sha256 = DigestUtils.sha256Hex(in);
                // Re-open stream (already consumed) for actual store:
                try (InputStream in2 = mf.getInputStream()) {

                    // If identical file already exists for this org, re-use it
                    Optional<FileUpload> existing = fileUploadRepository.findActiveByOrgAndSha(org.getOrgId(), sha256);
                    if (existing.isPresent()) {
                        out.add(mapper.toDTO(existing.get()));
                        continue;
                    }

                    String originalName = sanitize(mf.getOriginalFilename());
                    String safeName = uniqueName(sha256, originalName);
                    String contentType = safeContentType(mf.getContentType(), originalName);
                    long size = mf.getSize();

                    // 2) Store the binary (local/S3/etc.)
                    String fileUrl = storage.store(folder, safeName, in2, size, contentType);

                    // 3) Persist file_upload row
                    FileUpload entity = new FileUpload();
                    entity.setOrganization(org);
                    entity.setRelatedTable(relatedTable);
                    entity.setRelatedId(relatedId);
                    entity.setUploadedBy(uploadedBy);
                    entity.setFileUrl(fileUrl);
                    entity.setMimeType(contentType);
                    entity.setSizeBytes(size);
                    entity.setSha256(sha256);
                    entity.setStorageProvider(StorageProvider.LOCAL); // "LOCAL" / "S3" etc.
                    entity.setDateUpdated(java.time.LocalDateTime.now());
                    entity.setTags(mergedTags(baseTags, originalName, safeName));

                    // Infer FileType from mime/extension
                    entity.setFileType(guessType(contentType, originalName));

                    FileUpload saved;
                    try {
                        saved = fileUploadRepository.save(entity);
                    } catch (DataIntegrityViolationException dup) {
                        // unique constraint hit (org_id + sha256). Fetch existing & return it
                        saved = fileUploadRepository.findActiveByOrgAndSha(org.getOrgId(), sha256)
                                .orElseThrow(() -> dup);
                    }

                    out.add(mapper.toDTO(saved));
                }
            } catch (Exception e) {
                // You can choose to fail-fast or skip failed file & continue others.
                // Here we fail-fast to surface issues early:
                throw new RuntimeException("Failed to store file: " + mf.getOriginalFilename(), e);
            }
        }
        return out;
    }


    // ---------- helpers ----------
    private static String buildFolder(String relatedTable, UUID relatedId) {
        return relatedTable + "/" + relatedId + "/" + LocalDate.now();
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) return "file";
        // strip path segments and risky chars
        String base = name.replace("\\", "/");
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[\\r\\n]", "_");
        return base;
    }

    private static String uniqueName(String sha256, String originalName) {
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot > -1 && dot < originalName.length() - 1) {
            ext = originalName.substring(dot).toLowerCase(Locale.ROOT);
        }
        return sha256 + ext; // content-addressed
    }

    private static String safeContentType(String provided, String filename) {
        if (provided != null && !provided.isBlank()) return provided;
        // guess from extension
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF_VALUE;
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mov")) return "video/quicktime";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private static Map<String, Object> mergedTags(Map<String, Object> base, String original, String stored) {
        Map<String, Object> t = new HashMap<>(base);
        t.putIfAbsent("original_name", original);
        t.putIfAbsent("stored_name", stored);
        return t;
    }

    private static FileType guessType(String mime, String filename) {
        if (mime == null) mime = "";
        String m = mime.toLowerCase(Locale.ROOT);
        String f = filename.toLowerCase(Locale.ROOT);

        if (m.startsWith("image/") || f.matches(".*\\.(png|jpg|jpeg|gif|webp|bmp)$")) return FileType.PHOTO;
        if (m.startsWith("video/") || f.matches(".*\\.(mp4|mov|avi|mkv|webm)$")) return FileType.VIDEO;
        if (m.startsWith("audio/") || f.matches(".*\\.(mp3|wav|m4a|aac|ogg)$")) return FileType.AUDIO;
        if (f.endsWith(".pdf") || m.equals("application/pdf")) return FileType.DOCUMENT;
        // Default
        return FileType.DOCUMENT;
    }

    private String storageProviderName() {
        // If you have multiple impls, you can @Profile them and return "LOCAL"/"S3"/"GCS" here accordingly
        // For the LocalFileStorageService case:
        return "LOCAL";
    }


}
