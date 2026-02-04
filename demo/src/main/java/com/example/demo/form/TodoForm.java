package com.example.demo.form;

import lombok.Data;

@Data
public class TodoForm {

    private Long id;
    private String title;
    private String description;
    private Integer priority;
}
