package com.docmind.backend.service;

import com.docmind.backend.entity.DocumentMetadata;
import org.springframework.ai.document.Document;

import java.util.List;

public interface DocumentIngestionService {

    int ingest(DocumentMetadata documentMetadata, List<Document> parsedDocs);
}
