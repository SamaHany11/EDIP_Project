package com.example.EDIP.template.Controller;

import com.example.EDIP.template.Service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    @GetMapping
    public ResponseEntity<?> getTemplates() {
        return ResponseEntity.ok(List.of(
                Map.of(
                        "code", "LEAVE",
                        "title", "Leave Request",
                        "description", "Request time off for vacation or leave"
                ),
                Map.of(
                        "code", "TRANSFER",
                        "title", "Transfer Request",
                        "description", "Request transfer to another department"
                ),
                Map.of(
                        "code", "RESIGNATION",
                        "title", "Resignation Request",
                        "description", "Submit resignation request"
                )
        ));
    }
    @PostMapping("/leave")
    public ResponseEntity<?> submitLeave(@RequestBody Map<String, Object> body) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        templateService.submitTemplate(email, "LEAVE", body);

        return ResponseEntity.ok(Map.of("message", "The request has been submitted successfully."));
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> submitTransfer(@RequestBody Map<String, Object> body) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        templateService.submitTemplate(email, "TRANSFER", body);

        return ResponseEntity.ok(Map.of("message", "The request has been submitted successfully."));
    }

    @PostMapping("/resignation")
    public ResponseEntity<?> submitResignation(@RequestBody Map<String, Object> body) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        templateService.submitTemplate(email, "RESIGNATION", body);

        return ResponseEntity.ok(Map.of("message", "The request has been submitted successfully."));
    }


}