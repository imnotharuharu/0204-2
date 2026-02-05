package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
