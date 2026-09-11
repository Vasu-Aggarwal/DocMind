package com.docmind.backend.service;

import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentParserService {

    List<Document> parse(MultipartFile file);
}
