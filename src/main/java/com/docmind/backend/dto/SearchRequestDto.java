package com.docmind.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchRequestDto {

    @NotBlank(message = "Query cannot be empty")
    private String query;
    private UUID documentId;
    private int topK;
    private Double similaritySearch;

}
