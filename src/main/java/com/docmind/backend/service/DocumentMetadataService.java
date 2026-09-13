package com.docmind.backend.service;

import com.docmind.backend.dto.DocumentResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentMetadataService {

    DocumentResponseDto uploadAndProcess(MultipartFile file);

    List<DocumentResponseDto> uploadMultipleDocuments(List<MultipartFile> files);

    List<DocumentResponseDto> getAllDocuments();

    DocumentResponseDto getDocumentById(UUID id);

    void deleteDocumentById(UUID id);
}
