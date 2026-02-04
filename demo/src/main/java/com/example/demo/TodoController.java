package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TodoController {

    // Display the list of todos.
    @GetMapping("/todos")
    public String list() {
        return "todo/list";
    }

    // Show the form for creating a new todo.
    @GetMapping("/todos/new")
    public String newTodo() {
        return "todo/new";
    }

    // Display the details for a single todo by id.
    @GetMapping("/todos/{id}")
    public String detail(@PathVariable Long id) {
        return "todo/detail";
    }

    // Show the form for editing a todo by id.
    @GetMapping("/todos/{id}/edit")
    public String edit(@PathVariable Long id) {
        return "todo/edit";
    }
}
