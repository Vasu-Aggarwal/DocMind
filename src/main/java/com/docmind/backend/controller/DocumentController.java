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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    //upload multiple files
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload multiple files ", description = "")
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> uploadMultiple(
            @RequestParam("files")
            List<MultipartFile> files
    ){
        List<DocumentResponseDto> responseDtos = documentService.uploadMultipleDocuments(files);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<List<DocumentResponseDto>>builder()
                        .message("Documents Uploaded successfully")
                        .success(true)
                        .timestamp(LocalDateTime.now())
                        .data(responseDtos)
                .build());
    }

    //list all uploaded documents
    @GetMapping
    @Operation(summary = "List all uploaded documents and their indexing status")
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> getAllDocuments(){
        List<DocumentResponseDto> documents = documentService.getAllDocuments();
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<List<DocumentResponseDto>>builder()
                .message("Documents Fetched successfully")
                .success(true)
                .timestamp(LocalDateTime.now())
                .data(documents)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get metadata of a specific document by ID")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> getDocumentById(@PathVariable UUID id){
        DocumentResponseDto document = documentService.getDocumentById(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<DocumentResponseDto>builder()
                .message("Document Fetched successfully")
                .success(true)
                .timestamp(LocalDateTime.now())
                .data(document)
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a specific document by ID")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentById(@PathVariable UUID id){
        documentService.deleteDocumentById(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Void>builder()
                .message("Document deleted successfully")
                .success(true)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build());
    }

}
