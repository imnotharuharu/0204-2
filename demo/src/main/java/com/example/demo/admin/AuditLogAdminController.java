package com.example.demo.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;

@Controller
@RequestMapping("/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogAdminController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogAdminController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String list(
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String entityType,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Model model
    ) {
        String actionFilter = action != null ? action : "";
        String entityFilter = entityType != null ? entityType : "";
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> logs = auditLogRepository.findByActionContainingIgnoreCaseAndEntityTypeContainingIgnoreCase(
            actionFilter,
            entityFilter,
            pageable
        );
        model.addAttribute("logs", logs);
        model.addAttribute("action", action);
        model.addAttribute("entityType", entityType);
        return "admin/audit-logs";
    }
}
