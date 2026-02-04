package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    // Receive form data and show confirmation.
    @PostMapping("/todos/confirm")
    public String confirm(
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(defaultValue = "3") Integer priority,
        Model model
    ) {
        model.addAttribute("title", title);
        model.addAttribute("description", description);
        model.addAttribute("priority", priority);
        return "todo/confirm";
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
