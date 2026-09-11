package com.docmind.backend.service;

import com.docmind.backend.dto.DocumentResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentMetadataService {

    DocumentResponseDto uploadAndProcess(MultipartFile file);

}
