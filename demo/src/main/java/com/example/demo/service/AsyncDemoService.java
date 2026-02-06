package com.example.demo.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

import com.example.demo.service.exception.BusinessException;

@Service
public class AsyncDemoService {

    private final ReportService reportService;

    public AsyncDemoService(ReportService reportService) {
        this.reportService = reportService;
    }

    public String generateReportWithTimeout(Long userId) {
        CompletableFuture<String> future = reportService.generateTodoReport(userId);
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            throw new BusinessException("Report generation timed out");
        } catch (Exception ex) {
            throw new BusinessException("Report generation failed");
        }
    }

    public List<String> generateMultipleReports(Long userId) {
        CompletableFuture<String> r1 = reportService.generateTodoReport(userId);
        CompletableFuture<String> r2 = reportService.generateTodoReport(userId + 1);
        CompletableFuture.allOf(r1, r2).join();
        return List.of(r1.join(), r2.join());
    }
}
