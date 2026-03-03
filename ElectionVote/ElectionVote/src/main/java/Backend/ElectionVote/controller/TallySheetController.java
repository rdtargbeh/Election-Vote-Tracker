package Backend.ElectionVote.controller;


import Backend.ElectionVote.dto.TallySheetCreateByUrlRequest;
import Backend.ElectionVote.dto.TallySheetDto;
import Backend.ElectionVote.dto.TallySheetOcrUpdateRequest;
import Backend.ElectionVote.service.TallySheetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tally-sheets")
@RequiredArgsConstructor
public class TallySheetController {

    private final TallySheetService service;

    /** Multipart upload (recommended path) */
    @PostMapping(value = "/{submissionId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TallySheetDto upload(
            @PathVariable UUID submissionId,
            @RequestParam UUID orgId,
            @RequestPart("file") MultipartFile file) {
        return service.upload(orgId, submissionId, file);
    }

    /** Create by external URL (no file transfer) */
    @PostMapping("/by-url")
    public TallySheetDto createByUrl(@Valid @RequestBody TallySheetCreateByUrlRequest req) {
        return service.createByUrl(req);
    }

    /** Attach or replace OCR-extracted JSON */
    @PutMapping("/{uploadId}/ocr")
    public TallySheetDto setOcr(@PathVariable UUID uploadId,
                                @Valid @RequestBody TallySheetOcrUpdateRequest req) {
        return service.setOcr(uploadId, req.getOcrExtracted());
    }

    /** List all uploads for a submission */
    @GetMapping("/submission/{submissionId}")
    public List<TallySheetDto> listBySubmission(@PathVariable UUID submissionId) {
        return service.listBySubmission(submissionId);
    }

    @GetMapping("/{uploadId}")
    public TallySheetDto get(@PathVariable UUID uploadId) {
        return service.get(uploadId);
    }

    @DeleteMapping("/{uploadId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID uploadId) {
        service.delete(uploadId);
    }
}
