package com.example.demo.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import com.example.demo.entity.Priority;

@Data
public class TodoForm {

    private Long id;

    private Long version;

    @NotBlank
    @Size(max = 50)
    private String author;

    @NotBlank
    @Size(max = 100)
    private String title;

    @Size(max = 500)
    private String detail;

    @NotNull
    private Priority priority = Priority.MEDIUM;

    private Long categoryId;
}
