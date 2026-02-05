package com.example.demo.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

import lombok.Data;

import com.example.demo.entity.Priority;

@Data
public class TodoForm {

    private Long id;

    private Long version;

    @NotBlank(message = "{validation.notblank}")
    @Size(max = 50, message = "{validation.size50}")
    private String author;

    @NotBlank(message = "{validation.notblank}")
    @Size(max = 100, message = "{validation.size100}")
    private String title;

    @Size(max = 500, message = "{validation.size500}")
    private String detail;

    @NotNull(message = "{validation.notnull}")
    private Priority priority = Priority.MEDIUM;

    private Long categoryId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;
}
