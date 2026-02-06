package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String message) {
        AuditLog log = AuditLog.builder()
            .action(action)
            .entityType("SYSTEM")
            .message(message != null ? message : "")
            .build();
        auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDetailed(
        String action,
        String entityType,
        Long entityId,
        Long userId,
        String oldValue,
        String newValue,
        String ipAddress,
        String message
    ) {
        AuditLog log = AuditLog.builder()
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .userId(userId)
            .oldValue(oldValue)
            .newValue(newValue)
            .ipAddress(ipAddress)
            .message(message)
            .build();
        auditLogRepository.save(log);
    }
}
