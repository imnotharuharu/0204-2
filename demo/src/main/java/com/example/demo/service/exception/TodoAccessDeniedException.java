package com.example.demo.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TodoAccessDeniedException extends RuntimeException {

    public TodoAccessDeniedException() {
        super("Access denied");
    }
}
