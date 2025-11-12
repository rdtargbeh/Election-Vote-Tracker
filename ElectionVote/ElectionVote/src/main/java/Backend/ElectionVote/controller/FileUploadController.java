package Backend.ElectionVote.controller;


import Backend.ElectionVote.dto.FileUploadCreateRequest;
import Backend.ElectionVote.dto.FileUploadDto;
import Backend.ElectionVote.enums.FileType;
import Backend.ElectionVote.enums.StorageProvider;
import Backend.ElectionVote.service.FileUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/file-uploads")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService service;

    /** Multipart upload */
    @PostMapping(value = "/{relatedTable}/{relatedId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadDto upload(
            @PathVariable String relatedTable,
            @PathVariable UUID relatedId,
            @RequestParam UUID orgId,
            @RequestParam UUID uploadedBy,
            @RequestParam FileType fileType,
            @RequestParam StorageProvider storageProvider,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String mimeType,
            @RequestParam(required = false) Long sizeBytes
    ) {
        FileUploadCreateRequest meta = new FileUploadCreateRequest();
        meta.setOrgId(orgId);
        meta.setRelatedTable(relatedTable);
        meta.setRelatedId(relatedId);
        meta.setFileType(fileType);
        meta.setStorageProvider(storageProvider);
        meta.setMimeType(mimeType);
        meta.setSizeBytes(sizeBytes);
        return service.uploadMultipart(meta, file, uploadedBy);
    }

    /** Create by URL (no file body) */
    @PostMapping("/by-url")
    public FileUploadDto createByUrl(@Valid @RequestBody FileUploadCreateRequest req,
                                     @RequestParam UUID uploadedBy) {
        return service.createByUrl(req, uploadedBy);
    }

    /** List files attached to a record */
    @GetMapping
    public List<FileUploadDto> list(@RequestParam UUID orgId,
                                    @RequestParam String relatedTable,
                                    @RequestParam UUID relatedId) {
        return service.list(orgId, relatedTable, relatedId);
    }

    @GetMapping("/{fileId}")
    public FileUploadDto get(@PathVariable UUID fileId) { return service.get(fileId); }

    /** Soft delete */
    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID fileId, @RequestParam UUID requesterId) {
        service.softDelete(fileId, requesterId);
    }
}
