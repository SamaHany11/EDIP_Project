package com.example.EDIP.document.service;

import com.example.EDIP.document.exception.DocumentException;
import com.example.EDIP.document.model.sql.Attachment;
import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.document.repository.sql.AttachmentRepository;
import com.example.EDIP.document.repository.sql.DocumentRepository;
import com.example.EDIP.document.security.CryptoService;  // ✅ أضف هذا
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.example.EDIP.document.model.sql.DocumentReply;
import com.example.EDIP.document.repository.sql.DocumentReplyRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordPreviewService {

    private final GridFsTemplate gridFsTemplate;
    private final DocumentRepository documentRepository;
    private final AttachmentRepository attachmentRepository;
    private final CryptoService cryptoService;
    private final DocumentReplyRepository documentReplyRepository;
    public Map<String, Object> convertToHtml(
            UUID documentId,
            UUID attachmentId,
            UUID replyId
    ) {

        if (documentId != null && attachmentId != null) {
            throw new RuntimeException("Provide either documentId or attachmentId, not both");
        }

        String mongoFileId;
        String fileName;
        String type;
        UUID itemId;
        boolean isConfidential = false;

        if (documentId != null) {

            Document document = documentRepository
                    .findByDocumentIdAndIsDeletedFalse(documentId)
                    .orElseThrow(() -> DocumentException.notFound(documentId));

            if (!"WORD".equalsIgnoreCase(document.getDocumentFormat())) {
                throw new RuntimeException("This document is not a Word file");
            }

            mongoFileId = document.getMongoFileId();
            fileName = document.getFileName();
            type = "DOCUMENT";
            itemId = documentId;
            isConfidential = Boolean.TRUE.equals(document.getConfidential());

        } else if (attachmentId != null) {

            Attachment attachment = attachmentRepository
                    .findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("Attachment not found"));

            if (!isWordMimeType(attachment.getFileType())) {
                throw new RuntimeException("This attachment is not a Word file");
            }

            mongoFileId = attachment.getMongoFileId();
            fileName = attachment.getFileName();
            type = "ATTACHMENT";
            itemId = attachmentId;
            isConfidential = true;

        } else if (replyId != null) {

            DocumentReply reply = documentReplyRepository
                    .findById(replyId)
                    .orElseThrow(() ->
                            new RuntimeException("Reply not found"));

            if (!isWordMimeType(reply.getFileType())) {
                throw new RuntimeException("This reply file is not a Word file");
            }

            if (reply.getMongoFileId() == null) {
                throw new RuntimeException("Reply has no file");
            }

            mongoFileId = reply.getMongoFileId();
            fileName = reply.getFileName();
            type = "REPLY";
            itemId = replyId;


            isConfidential = false;

        } else {

            throw new RuntimeException(
                    "Either documentId or attachmentId or replyId must be provided"
            );
        }

        try {
            XWPFDocument wordDoc = loadWordFromMongo(mongoFileId, isConfidential);
            String html = buildHtmlFromWord(wordDoc);
            wordDoc.close();

            Map<String, Object> result = new HashMap<>();
            result.put("id", itemId);
            result.put("type", type);
            result.put("fileName", fileName);
            result.put("html", html);

            return result;

        } catch (IOException e) {
            log.error("Word preview failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to preview document");
        }
    }


    private XWPFDocument loadWordFromMongo(String mongoFileId, boolean isEncrypted) throws IOException {

        GridFSFile gridFSFile = gridFsTemplate.findOne(
                new Query(Criteria.where("_id").is(new ObjectId(mongoFileId)))
        );

        if (gridFSFile == null) {
            throw DocumentException.invalidFile("File not found");
        }

        GridFsResource resource = gridFsTemplate.getResource(gridFSFile);

        try (InputStream is = resource.getInputStream()) {
            byte[] fileBytes = is.readAllBytes();


            if (isEncrypted) {
                fileBytes = cryptoService.decrypt(fileBytes);
            }

            return new XWPFDocument(new java.io.ByteArrayInputStream(fileBytes));
        }
    }


    private String buildHtmlFromWord(XWPFDocument wordDoc) {

        StringBuilder html = new StringBuilder();

        html.append("<div style='font-family: Arial; line-height:1.6;'>");

        // Paragraphs
        for (XWPFParagraph para : wordDoc.getParagraphs()) {

            if (para.getText() == null || para.getText().trim().isEmpty()) continue;

            html.append("<p style='")
                    .append(getAlignment(para))
                    .append("margin-left:")
                    .append(para.getIndentationLeft() / 567.0)
                    .append("cm;'>");

            for (XWPFRun run : para.getRuns()) {
                String text = run.getText(0);
                if (text != null) {
                    html.append(formatRun(text, run));
                }
            }

            html.append("</p>");
        }

        // Tables
        for (XWPFTable table : wordDoc.getTables()) {

            html.append("<table style='border-collapse:collapse;width:100%;'>");

            for (XWPFTableRow row : table.getRows()) {
                html.append("<tr>");

                for (XWPFTableCell cell : row.getTableCells()) {
                    html.append("<td style='border:1px solid #ddd;padding:8px;'>");

                    for (XWPFParagraph p : cell.getParagraphs()) {
                        html.append(StringEscapeUtils.escapeHtml4(p.getText()));
                    }

                    html.append("</td>");
                }

                html.append("</tr>");
            }

            html.append("</table>");
        }

        html.append("</div>");
        return html.toString();
    }

    private String getAlignment(XWPFParagraph para) {

        if (para.getAlignment() == null) return "text-align:left;";

        return switch (para.getAlignment()) {
            case CENTER -> "text-align:center;";
            case RIGHT -> "text-align:right;";
            case BOTH -> "text-align:justify;";
            default -> "text-align:left;";
        };
    }


    private String formatRun(String text, XWPFRun run) {

        String safe = StringEscapeUtils.escapeHtml4(text);
        StringBuilder out = new StringBuilder();

        if (run.isBold()) out.append("<strong>");
        if (run.isItalic()) out.append("<em>");
        if (run.getUnderline() != UnderlinePatterns.NONE) out.append("<u>");

        if (run.getColor() != null && !"auto".equals(run.getColor())) {
            out.append("<span style='color:#").append(run.getColor()).append(";'>");
        }

        out.append(safe);

        if (run.getColor() != null && !"auto".equals(run.getColor())) {
            out.append("</span>");
        }

        if (run.getUnderline() != UnderlinePatterns.NONE) out.append("</u>");
        if (run.isItalic()) out.append("</em>");
        if (run.isBold()) out.append("</strong>");

        return out.toString();
    }
    private boolean isWordMimeType(String mimeType) {
        return mimeType != null && (
                mimeType.equals("application/msword") ||
                        mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
    }
}