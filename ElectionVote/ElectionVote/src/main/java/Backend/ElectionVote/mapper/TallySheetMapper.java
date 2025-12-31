package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.TallySheetDto;
import Backend.ElectionVote.entity.TallySheet;
import org.springframework.stereotype.Component;

@Component
public class TallySheetMapper {
    public TallySheetDto toDTO(TallySheet t) {
        return TallySheetDto.builder()
                .uploadId(t.getUploadId())
                .orgId(t.getOrganization().getOrgId())
                .submissionId(t.getSubmission().getSubmissionId())
                .imageUrl(t.getImageUrl())
                .fileSha256(t.getFileSha256())
                .dateUploaded(t.getDateUploaded())
                .lastUpdated(t.getLastUpdated())
                .ocrExtracted(t.getOcrExtracted())
                .build();
    }
}
