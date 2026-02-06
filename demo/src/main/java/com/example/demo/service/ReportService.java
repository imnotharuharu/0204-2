package com.example.demo.service;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    @Async("taskExecutor")
    public CompletableFuture<String> generateTodoReport(Long userId) {
        try {
            Thread.sleep(300);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Report generation interrupted", ex);
        }
        String result = "report-for-user-" + userId;
        log.info("Generated report {}", result);
        return CompletableFuture.completedFuture(result);
    }
}
