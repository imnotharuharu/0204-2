package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.form.TodoForm;
import com.example.demo.service.TodoService;
import com.example.demo.service.exception.TodoNotFoundException;

@Controller
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    // Display the list of todos.
    @GetMapping("/todos")
    public String list(Model model) {
        model.addAttribute("todos", todoService.findAllOrderByCreatedAtDesc());
        return "todo/list";
    }

    // Show the form for creating a new todo.
    @GetMapping("/todos/new")
    public String newTodo() {
        return "todo/form";
    }

    // Receive form data and show confirmation.
    @PostMapping("/todos/confirm")
    public String confirm(@ModelAttribute TodoForm todoForm, Model model) {
        model.addAttribute("todoForm", todoForm);
        return "todo/confirm";
    }

    // Allow direct access to confirmation page (GET).
    @GetMapping("/todos/confirm")
    public String confirmPage(Model model) {
        model.addAttribute("todoForm", new TodoForm());
        return "todo/confirm";
    }

    // Receive hidden fields from confirmation and complete registration.
    @PostMapping("/todos/complete")
    public String complete(@ModelAttribute TodoForm todoForm, RedirectAttributes redirectAttributes) {
        todoService.createFromForm(todoForm);
        redirectAttributes.addFlashAttribute("successMessage", "登録が完了しました");
        return "redirect:/todos";
    }

    // Delete a todo by id.
    @PostMapping("/todos/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            todoService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "削除が完了しました");
        } catch (TodoNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "指定されたToDoが見つかりません");
        }
        return "redirect:/todos";
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
