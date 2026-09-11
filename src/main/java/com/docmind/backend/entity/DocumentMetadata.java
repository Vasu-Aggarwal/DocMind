package com.docmind.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_metadata")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String filename;
    private String contentType;
    private Long fileSize;
    private Integer totalPages;
    private Integer totalChunks;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Column(length = 1000)
    private String errorMessage;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
