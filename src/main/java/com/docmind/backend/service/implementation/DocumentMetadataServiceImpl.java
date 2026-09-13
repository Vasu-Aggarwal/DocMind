package com.docmind.backend.service.implementation;

import com.docmind.backend.dto.DocumentResponseDto;
import com.docmind.backend.entity.DocumentMetadata;
import com.docmind.backend.entity.DocumentStatus;
import com.docmind.backend.exception.ResourceNotFoundException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public List<DocumentResponseDto> uploadMultipleDocuments(List<MultipartFile> files) {

        List<DocumentResponseDto> responseDtos = new ArrayList<>();
        for(MultipartFile file: files){
            DocumentResponseDto result = this.uploadAndProcess(file);
            responseDtos.add(result);
        }

        return responseDtos;
    }

    @Override
    public List<DocumentResponseDto> getAllDocuments() {
        List<DocumentMetadata> fetchedDocuments = documentMetadataRepo.findAllByOrderByCreatedAtDesc();
        return fetchedDocuments.stream().map(this::mapToDto).toList();
    }

    private DocumentResponseDto mapToDto(DocumentMetadata documentMetadata) {
        return DocumentResponseDto
                .builder()
                .message(documentMetadata.getErrorMessage())
                .id(documentMetadata.getId())
                .fileSize(documentMetadata.getFileSize())
                .fileName(documentMetadata.getFilename())
                .chunksCreated(documentMetadata.getTotalChunks())
                .status(documentMetadata.getStatus())
                .build();
    }

    @Override
    public DocumentResponseDto getDocumentById(UUID id) {
        DocumentMetadata documentMetadata = documentMetadataRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document with id: " + id + " not found!"));

        return DocumentResponseDto
                .builder()
                .message(documentMetadata.getErrorMessage())
                .id(documentMetadata.getId())
                .fileSize(documentMetadata.getFileSize())
                .fileName(documentMetadata.getFilename())
                .chunksCreated(documentMetadata.getTotalChunks())
                .status(documentMetadata.getStatus())
                .build();
    }

    @Transactional
    public void deleteDocumentById(UUID id) {
        DocumentMetadata documentMetadata = documentMetadataRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document with given id not found !!"));

        documentMetadataRepo.delete(documentMetadata);
        //delete the vector entries
        try {
            String deleteVectorsSql = "DELETE FROM vector_store WHERE metadata->>'documentId' = ?";
            int deletedCount = jdbcTemplate.update(deleteVectorsSql, id.toString());
            logger.info("Deleted {} vector chunks for document id {} ", deletedCount, id);

        } catch (Exception e) {
            logger.warn("cloud not delete vectors from vector store directly: {}", e.getMessage());
        }

    }
}
