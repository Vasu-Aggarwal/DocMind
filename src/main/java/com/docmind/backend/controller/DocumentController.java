package com.docmind.backend.controller;

import com.docmind.backend.dto.ApiResponse;
import com.docmind.backend.dto.DocumentResponseDto;
import com.docmind.backend.service.DocumentMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController()
@RequestMapping("/api/v1/documents")
@Tag(name="Document Management", description = "Endpoints for uploading doc, listing and managing document and their vector embeddings")
@RequiredArgsConstructor
public class DocumentController {

    @Autowired
    private DocumentMetadataService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and index a document")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadDocumentMetadata(@RequestParam("File") MultipartFile file){

        DocumentResponseDto documentResponseDto = this.documentService.uploadAndProcess(file);

        return new ResponseEntity<>(ApiResponse.<DocumentResponseDto>builder()
                .success(true)
                .message("File Indexed successfully !!")
                .data(documentResponseDto)
                .timestamp(LocalDateTime.now())
                .build(), HttpStatus.CREATED);
    }

}
