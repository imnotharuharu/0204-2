package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TodoController {

    // Display the list of todos.
    @GetMapping("/todos")
    public String list() {
        return "todos/list";
    }

    // Show the form for creating a new todo.
    @GetMapping("/todos/new")
    public String newTodo() {
        return "todos/new";
    }

    // Display the details for a single todo by id.
    @GetMapping("/todos/{id}")
    public String detail(@PathVariable Long id) {
        return "todos/detail";
    }
}
