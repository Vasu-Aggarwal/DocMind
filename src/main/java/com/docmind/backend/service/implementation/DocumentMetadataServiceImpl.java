package com.docmind.backend.service.implementation;

import com.docmind.backend.dto.DocumentResponseDto;
import com.docmind.backend.entity.DocumentMetadata;
import com.docmind.backend.entity.DocumentStatus;
import com.docmind.backend.repository.DocumentMetadataRepo;
import com.docmind.backend.service.DocumentMetadataService;
import com.docmind.backend.service.DocumentIngestionService;
import com.docmind.backend.service.DocumentParserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentMetadataServiceImpl implements DocumentMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentMetadataServiceImpl.class);
    private final DocumentMetadataRepo documentMetadataRepo;
    private final DocumentParserService parserService;
    private final DocumentIngestionService ingestionService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public DocumentResponseDto uploadAndProcess(MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octat-stream";

        //document metadata creation
        DocumentMetadata documentMetadata = DocumentMetadata.builder()
                .filename(filename)
                .contentType(contentType)
                .status(DocumentStatus.UPLOADING)
                .fileSize(file.getSize())
                .createdAt(LocalDateTime.now())
                .build();

        //save the document metadata
        documentMetadata = documentMetadataRepo.save(documentMetadata);

        //parse the document
        List<Document> parsedDocs = parserService.parse(file);

        //ingest the parsed docs
        int chunksCreated = ingestionService.ingest(documentMetadata, parsedDocs);

        return DocumentResponseDto.builder()
                .id(documentMetadata.getId())
                .fileName(documentMetadata.getFilename())
                .fileSize(documentMetadata.getFileSize())
                .chunksCreated(chunksCreated)
                .status(documentMetadata.getStatus())
                .message("Document successfully processed and indexed.")
                .build();
    }
}
