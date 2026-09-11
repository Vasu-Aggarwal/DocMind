package com.docmind.backend.service;

import com.docmind.backend.dto.*;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.UUID;

public interface RagService {

    SearchResultDto searchSimilarChunks(SearchRequestDto request);
    CitationDto mapToCitation(Document document);
    List<Document> retrieveRelevantDocuments(String query, UUID documentId, Integer topK, Double similaritySearch);
    ChatResponseDto askQuestion(ChatRequestDto request);
}
