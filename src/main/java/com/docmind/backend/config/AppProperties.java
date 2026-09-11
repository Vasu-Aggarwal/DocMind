package com.docmind.backend.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private RagProperties rag = new RagProperties();
    private CorsProperties cors = new CorsProperties();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RagProperties {
        /**
         * Target maximum size of a chunk, therefore, if we have 5000 tokens then there will be multiple chunks created each with 600 tokens, resulting in approx 9 chunks
         */
        private int chunkSize = 600;

        /**
         * This is characters, not tokens. This won't stop till we have 350 characters and after that it will stop at the first punctuation mark.
         */
        private int minChunkSizeChar = 350;
//        private int chunkOverlap = 100;       // not supported in spring AI tokenTextSplitter
        /**
         * Minimum number of characters required to create a chunk
         */
        private int minChunkLengthToEmbed = 5;
        private int maxNumChunks = 10000;
        private int topK = 5;
        private double similarityThreshold = 0.0;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CorsProperties {
        private String allowedOrigins = "*";
        private String allowedMethods = "GET, POST, DELETE, PUT, PATCH, OPTIONS";
        private String allowedHeaders = "*";

    }

}
