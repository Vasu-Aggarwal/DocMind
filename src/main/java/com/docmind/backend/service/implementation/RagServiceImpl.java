package com.docmind.backend.service.implementation;

import com.docmind.backend.config.AppProperties;
import com.docmind.backend.dto.*;
import com.docmind.backend.service.RagService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagServiceImpl.class);
    private final VectorStore vectorStore;
    private final AppProperties appProperties;
    private final ChatClient chatClient;

    @Override
    public ChatResponseDto askQuestion(ChatRequestDto request){
        long startTime = System.currentTimeMillis();
        logger.info("Processing query: '{}', scoped documentId: {}", request.getQuestion(), request.getDocumentId());
        List<Document> similarDocuments = this.retrieveRelevantDocuments(request.getQuestion(), request.getDocumentId(), request.getTopK(), request.getMinSimilarity());


        List<CitationDto> citationDtos = similarDocuments.stream().map(this::mapToCitation).toList();

        String contextText = buildContextString(similarDocuments);

        String prompt = buildPrompt(request.getQuestion(), contextText);

        String answer = this.chatClient.prompt().user(prompt).call().content();
        long responseTime = System.currentTimeMillis() - startTime;
        logger.info("Completed Q&A in {} ms with {} citations", responseTime, citationDtos.size());
        return ChatResponseDto.builder().answer(answer).conversationId(request.getConversationId() != null ? request.getConversationId() : UUID.randomUUID().toString()).citations(citationDtos).responseTimeMs(responseTime).build();

    }

    @Override
    public Flux<String> streamQuestionAnswer(ChatRequestDto requestDto) {
        logger.info("Streaming query: '{}'", requestDto.getQuestion());
        List<Document> relevantDocuments = retrieveRelevantDocuments(
                requestDto.getQuestion(),
                requestDto.getDocumentId(),
                requestDto.getTopK(),
                requestDto.getMinSimilarity()
        );
        String contextText = buildContextString(relevantDocuments);
        String userPrompt = buildPrompt(requestDto.getQuestion(), contextText);
        return chatClient.prompt()
                .user(userPrompt)
                .stream()
                .content();
    }

    @Override
    public String buildPrompt(@NotBlank(message = "Question cannot be empty") String question, String contextText) {
        if (contextText != null && !contextText.isBlank()) {
            return String.format("""
                           Document Context:
                           ---------------------
                           %s
                           ---------------------                                        
                           User Message / Question: %s             
                           Instructions:
                           - If the user's question relates to the document context above, prioritize answering using that context and reference key sections.
                           - If the user is asking a general question, greeting, or discussing topics beyond the document context, respond helpfully and conversationally using your general knowledge while weaving in relevant document context if applicable
                    
                    """, contextText, question);
        } else {
            return String.format("""
                       User Message / Question:
                       %s                         
                       Instructions:
                          - Respond helpfully, accurately, and conversationally to the user's message using your broad knowledge base.
                    
                    """, question);
        }
    }

    private String buildContextString(List<Document> similarDocuments) {
        if (similarDocuments == null || similarDocuments.isEmpty()) {
            return "";
        }

        return similarDocuments.stream().map(doc -> {
            String fileName = (String) doc.getMetadata().getOrDefault("fileName", "Unknown File");
            Object page = doc.getMetadata().getOrDefault("pageNumber", "N/A");
            return String.format("[Source: %s | Page: %s]\n%s", fileName, page, doc.getText());
        }).collect(Collectors.joining("\n\n---\n\n"));
    }

    @Override
    public SearchResultDto searchSimilarChunks(SearchRequestDto request) {

        List<Document> matchedDocs = retrieveRelevantDocuments(request.getQuery(), request.getDocumentId(), request.getTopK(), request.getSimilaritySearch());

        List<CitationDto> citations = matchedDocs.stream().map(this::mapToCitation).toList();
        return SearchResultDto.builder()
                .query(request.getQuery())
                .totalMatches(citations.size())
                .matches(citations)
                .build();
    }

    @Override
    public CitationDto mapToCitation(Document document) {

        Map<String, Object> meta = document.getMetadata();
        UUID docId = null;
        if(meta.get("documentId") != null){
            try{
                docId = UUID.fromString(meta.get("documentId").toString());
            } catch(Exception ignore){

            }
        }

        Integer chunkIndex = null, pageNumber = null;
        if(meta.get("chunkIndex") instanceof Number n){
            chunkIndex = n.intValue();
        }

        if(meta.get("pageNumber") instanceof Number n){
            pageNumber = n.intValue();
        } else if(meta.get("page_number") instanceof Number n){
            pageNumber = n.intValue();
        }

        Double score = null;
        if(meta.get("distance") instanceof Number n){
            score = 1.0 - n.doubleValue();
        }

        return CitationDto.builder().documentId(docId).fileName((String) meta.getOrDefault("fileName", "Unknown")).chunkIndex(chunkIndex).pageNumber(pageNumber).snippet(document.getText()).similarityScore(score).metadata(meta).build();
    }

    @Override
    public List<Document> retrieveRelevantDocuments(@NotBlank(message = "Query cannot be empty") String query, UUID documentId, Integer topK, Double similaritySearch) {

        int effectiveTopK = topK != null && topK > 0 ? topK : appProperties.getRag().getTopK();
        double effectiveSimilarity = (similaritySearch != null && similaritySearch > 0 ? similaritySearch : appProperties.getRag().getSimilarityThreshold());

        SearchRequest.Builder searchRequest = SearchRequest.builder()
                .query(query)
                .topK(effectiveTopK);

        if(effectiveSimilarity > 0.0){
            searchRequest.similarityThreshold(effectiveSimilarity);
        }

        if(documentId != null){
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            searchRequest.filterExpression(b.eq("documentId", documentId.toString()).build());
        }

        try{
            List<Document> documents = vectorStore.similaritySearch(searchRequest.build());
            logger.info("Retrieved {} chunks for query: '{}' (scoped docId: {})", documents.size(), query, documentId);
            return documents;
        } catch (Exception e){
            logger.error("Similarity search failed for query: '{}'", query, e);
            return Collections.emptyList();
        }
    }
}
