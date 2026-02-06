package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Async("emailExecutor")
    public void sendTodoCreatedEmail(String to, Long todoId) {
        log.info("Sending email to {} for todo {}", to, todoId);
    }
}
