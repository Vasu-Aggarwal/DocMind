package com.docmind.backend.repository;

import com.docmind.backend.entity.DocumentMetadata;
import com.docmind.backend.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentMetadataRepo extends JpaRepository<DocumentMetadata, UUID> {

    List<DocumentMetadata> findByStatus(DocumentStatus status);
    List<DocumentMetadata> findAllByOrderByCreatedAtDesc();
}
