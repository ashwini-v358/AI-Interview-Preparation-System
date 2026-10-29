package com.smartinterview.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * PdfExtractionService — extracts plain text from PDF files using Apache PDFBox 3.x.
 *
 * Concepts:
 * - PDFBox: Apache's open-source PDF processing library.
 * - PDDocument: Represents a loaded PDF file in memory.
 * - PDFTextStripper: Walks the PDF's content stream and extracts text in reading order.
 * - We process only the file's InputStream (never write it to disk here) to keep
 *   the extraction pipeline stateless and testable.
 *
 * Why isolate this?
 * - AI services need clean text, not binary PDF bytes.
 * - PDFBox can throw if the PDF is malformed; catching here means the Service
 *   layer always receives either valid text or a clear exception — never a binary blob.
 */
@Slf4j
@Service
public class PdfExtractionService {

    private static final int MAX_PAGES = 50; // Safety cap — resumes rarely exceed 5 pages

    /**
     * Extracts raw text from a PDF MultipartFile.
     *
     * @param file the uploaded PDF file.
     * @return extracted plain text, or empty string if extraction fails gracefully.
     * @throws IllegalArgumentException if the file is not a valid PDF.
     */
    public String extractText(MultipartFile file) {
        validatePdfFile(file);
        try (InputStream is = file.getInputStream()) {
            return extractFromStream(is, file.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PDF file: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * Extracts raw text from a PDF at a given filesystem path.
     */
    public String extractTextFromPath(java.nio.file.Path path) {
        try (InputStream is = java.nio.file.Files.newInputStream(path)) {
            return extractFromStream(is, path.getFileName().toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PDF from path: " + path, e);
        }
    }

    private String extractFromStream(InputStream is, String filename) throws IOException {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(is.readAllBytes())) {
            int pageCount = document.getNumberOfPages();
            log.info("Extracting text from PDF '{}' ({} pages)", filename, pageCount);

            if (pageCount == 0) {
                log.warn("PDF '{}' has no pages", filename);
                return "";
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(pageCount, MAX_PAGES));
            stripper.setSortByPosition(true); // Improves column layout parsing

            String text = stripper.getText(document);
            log.info("Extracted {} characters from '{}'", text.length(), filename);
            return text.trim();
        }
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file is empty or missing");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw new IllegalArgumentException(
                    "Only PDF files are accepted. Received: " + contentType);
        }
        if (file.getSize() > 10 * 1024 * 1024) { // 10 MB
            throw new IllegalArgumentException("PDF file size exceeds 10 MB limit");
        }
    }
}
