package com.docmind.backend.service.implementation;

import com.docmind.backend.exception.DocumentProcessingException;
import com.docmind.backend.service.DocumentParserService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class DocumentParserServiceImpl implements DocumentParserService {

    private final static Logger logger = LoggerFactory.getLogger(DocumentParserServiceImpl.class);

    @Override
    public List<Document> parse(MultipartFile file) {

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String contentType = file.getContentType() != null ? file.getContentType() : "";

        logger.info("Parsing document: {}, size: {} bytes, contentType: {}", filename, file.getSize(), contentType);

        try{

            Resource resource = new ByteArrayResource(file.getBytes()){
                @Override
                public @Nullable String getFilename() {
                    return filename;
                }
            };

            if(filename.toLowerCase().endsWith(".pdf") || contentType.contains("pdf")){
                return parsePdf(resource);
            } else {
                return parseGenericFile(resource);
            }

        } catch (IOException ex){
            logger.error("Failed to read file bytes {}", filename, ex);
            throw new DocumentProcessingException("Could not read uploaded file : "+filename, ex);
        } catch (Exception ex){
            logger.error("Error during document parsing {}", filename, ex);
            throw new DocumentProcessingException("Could not read uploaded file : "+filename, ex);
        }
    }

    private List<Document> parseGenericFile(Resource resource) {

        PdfDocumentReaderConfig config = PdfDocumentReaderConfig
                .builder()
                .withPageTopMargin(0)
                .withPageBottomMargin(0)
                .build();
        PagePdfDocumentReader documentReader = new PagePdfDocumentReader(resource, config);
        return documentReader.read();
    }

    private List<Document> parsePdf(Resource resource) {
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
        return tikaDocumentReader.read();
    }
}
