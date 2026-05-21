package com.example.EDIP.document.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

import java.util.UUID;

@Getter
public class DocumentException extends RuntimeException {

    private final HttpStatus status;

    public DocumentException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }


    public static DocumentException notFound(UUID documentId) {
        return new DocumentException(
                "Document not found: " + documentId,
                HttpStatus.NOT_FOUND
        );
    }

    public static DocumentException accessDenied() {
        return new DocumentException(
                "You don't have access to this document",
                HttpStatus.FORBIDDEN
        );
    }

    public static DocumentException invalidFile(String reason) {
        return new DocumentException(
                "Invalid file: " + reason,
                HttpStatus.BAD_REQUEST
        );
    }

    public static DocumentException cannotForwardOutsideDepartment() {
        return new DocumentException(
                "Employee cannot forward document outside department",
                HttpStatus.FORBIDDEN
        );
    }

    public static DocumentException noReplyContent() {
        return new DocumentException(
                "Reply must have text or file",
                HttpStatus.BAD_REQUEST
        );
    }
}