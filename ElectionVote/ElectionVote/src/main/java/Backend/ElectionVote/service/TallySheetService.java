package Backend.ElectionVote.service;


import Backend.ElectionVote.dto.TallySheetCreateByUrlRequest;
import Backend.ElectionVote.dto.TallySheetDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface TallySheetService {

    TallySheetDto upload(UUID orgId, UUID submissionId, MultipartFile file);

    TallySheetDto createByUrl(TallySheetCreateByUrlRequest req);

    TallySheetDto setOcr(UUID uploadId, String ocrJson);

    List<TallySheetDto> listBySubmission(UUID submissionId);

    void delete(UUID uploadId);

    TallySheetDto get(UUID uploadId);

}
