package com.example.demo.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

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

    @Min(1)
    @Max(5)
    private Integer priority;
}
