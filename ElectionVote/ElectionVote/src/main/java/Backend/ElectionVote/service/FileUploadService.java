package Backend.ElectionVote.service;


import Backend.ElectionVote.dto.FileUploadCreateRequest;
import Backend.ElectionVote.dto.FileUploadDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface FileUploadService {

    FileUploadDto uploadMultipart(FileUploadCreateRequest meta, MultipartFile file, UUID uploadedBy);

    FileUploadDto createByUrl(FileUploadCreateRequest req, UUID uploadedBy);

    List<FileUploadDto> list(UUID orgId, String relatedTable, UUID relatedId);

    void softDelete(UUID fileId, UUID requesterId);

    FileUploadDto get(UUID fileId);

}

