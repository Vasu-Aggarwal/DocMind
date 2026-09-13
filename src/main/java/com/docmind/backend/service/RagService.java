package com.docmind.backend.service;

import com.docmind.backend.dto.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public interface RagService {

    Flux<String> streamQuestionAnswer(ChatRequestDto requestDto);

    String buildPrompt(@NotBlank(message = "Question cannot be empty") String question, String contextText);

    SearchResultDto searchSimilarChunks(SearchRequestDto request);
    CitationDto mapToCitation(Document document);
    List<Document> retrieveRelevantDocuments(String query, UUID documentId, Integer topK, Double similaritySearch);
    ChatResponseDto askQuestion(ChatRequestDto request);
}
