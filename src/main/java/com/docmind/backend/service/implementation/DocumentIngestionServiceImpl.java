package com.docmind.backend.service.implementation;

import com.docmind.backend.config.AppProperties;
import com.docmind.backend.entity.DocumentMetadata;
import com.docmind.backend.entity.DocumentStatus;
import com.docmind.backend.exception.DocumentProcessingException;
import com.docmind.backend.repository.DocumentMetadataRepo;
import com.docmind.backend.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIngestionServiceImpl.class);

    private final VectorStore vectorStore;
    private final DocumentMetadataRepo documentMetadataRepo;
    private final AppProperties appProperties;

    @Override
    public int ingest(DocumentMetadata documentMetadata, List<Document> parsedDocs) {
        logger.info("Ingesting document [id={}, name={}, pages={}]", documentMetadata.getId(), documentMetadata.getFilename(), documentMetadata.getTotalPages());

        try{
            documentMetadata.setStatus(DocumentStatus.PROCESSING);
            documentMetadata.setTotalPages(parsedDocs.size());

            //1. text chunking using tokentextsplitter

            TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                    .withChunkSize(appProperties.getRag().getChunkSize())
                    .withMinChunkSizeChars(appProperties.getRag().getMinChunkSizeChar())
                    .withMinChunkLengthToEmbed(appProperties.getRag().getMinChunkLengthToEmbed())
                    .withMaxNumChunks(appProperties.getRag().getMaxNumChunks())
                    .withKeepSeparator(true)
                    .build();

            List<Document> chunks = tokenTextSplitter.apply(parsedDocs);
            if(chunks.isEmpty()){
                documentMetadata.setStatus(DocumentStatus.FAILED);
                documentMetadata.setErrorMessage("Document appears to be empty or unscannable ");
                documentMetadataRepo.save(documentMetadata);
                return 0;
            }

            //2. Meta data enrichment on each chunk
            List<Document> enrichedChunks = new ArrayList<>();
            for(int i=0;i<chunks.size();i++){
                Document chunk = chunks.get(i);
                Map<String, Object> enrichedMetadata = new HashMap<>(chunk.getMetadata());
                enrichedMetadata.put("documentId", documentMetadata.getId().toString());
                enrichedMetadata.put("fileName", documentMetadata.getFilename());
                enrichedMetadata.put("contentType", documentMetadata.getContentType());
                enrichedMetadata.put("chunkIndex", i);

                Object pageNumber = chunk.getMetadata().get("page_number");
                if (pageNumber == null){
                    pageNumber = chunk.getMetadata().get("pageNumber");
                }
                if (pageNumber != null){
                    enrichedMetadata.put("pageNumber", pageNumber);
                }

                Document enrichedDoc = new Document(chunk.getText(), enrichedMetadata);
                enrichedChunks.add(enrichedDoc);
            }

            //3. write chunks and embedding to pg vector
            logger.info("Writing {} vector chunks to PgVectorStore for document: {}", enrichedChunks.size(), documentMetadata.getFilename());

            vectorStore.add(enrichedChunks);

            //4. update document status to index
            documentMetadata.setStatus(DocumentStatus.INDEXED);
            documentMetadata.setTotalChunks(enrichedChunks.size());
            documentMetadata.setErrorMessage(null);
            documentMetadataRepo.save(documentMetadata);
            logger.info("Successfully indexed document [id={}, name={}, chunks={}]", documentMetadata.getId(), documentMetadata.getFilename(), enrichedChunks.size());

            return enrichedChunks.size();
        } catch(Exception e){
            logger.error("failed to ingest document into vector store: {}\nmetadata: {}", e.getMessage(), documentMetadata);
            documentMetadata.setStatus(DocumentStatus.FAILED);
            documentMetadata.setErrorMessage(e.getMessage());
            documentMetadataRepo.save(documentMetadata);
            throw new DocumentProcessingException("Failed to index document "+e.getMessage(), e);
        }
    }
}
