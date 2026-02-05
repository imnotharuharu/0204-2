package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableDefault;

import jakarta.validation.Valid;

import com.example.demo.entity.Todo;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.form.TodoForm;
import com.example.demo.service.TodoService;
import com.example.demo.service.exception.TodoNotFoundException;

@Controller
public class TodoController {

    private final TodoService todoService;
    private final CategoryRepository categoryRepository;

    public TodoController(TodoService todoService, CategoryRepository categoryRepository) {
        this.todoService = todoService;
        this.categoryRepository = categoryRepository;
    }

    // Display the list of todos.
    @GetMapping("/todos")
    public String list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) String direction,
        @RequestParam(required = false) Long categoryId,
        @PageableDefault(size = 10) Pageable pageable,
        Model model
    ) {
        String sortKey = resolveSortKey(sort);
        Sort.Direction dir = resolveDirection(direction);
        Sort sortSpec = Sort.by(dir, sortKey);
        Pageable request = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortSpec);
        Page<Todo> page;

        if (keyword != null && !keyword.isBlank()) {
            page = todoService.searchByTitle(keyword, request, categoryId);
        } else {
            page = todoService.findAll(request, categoryId);
        }

        long total = page.getTotalElements();
        int start = total == 0 ? 0 : page.getNumber() * page.getSize() + 1;
        int end = total == 0 ? 0 : page.getNumber() * page.getSize() + page.getNumberOfElements();

        model.addAttribute("todos", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sortKey);
        model.addAttribute("direction", dir.name().toLowerCase());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("name")));
        model.addAttribute("rangeStart", start);
        model.addAttribute("rangeEnd", end);
        model.addAttribute("totalElements", total);
        return "todo/list";
    }

    private String resolveSortKey(String sort) {
        if ("title".equals(sort) || "completed".equals(sort) || "createdAt".equals(sort) || "priority".equals(sort)) {
            return sort;
        }
        return "createdAt";
    }

    private Sort.Direction resolveDirection(String direction) {
        if ("asc".equalsIgnoreCase(direction)) {
            return Sort.Direction.ASC;
        }
        return Sort.Direction.DESC;
    }

    // Show the form for creating a new todo.
    @GetMapping("/todos/new")
    public String newTodo(Model model) {
        model.addAttribute("todoForm", new TodoForm());
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("name")));
        return "todo/form";
    }

    // Receive form data and show confirmation.
    @PostMapping("/todos/confirm")
    public String confirm(@Valid @ModelAttribute TodoForm todoForm, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAll(Sort.by("name")));
            return "todo/form";
        }
        model.addAttribute("todoForm", todoForm);
        if (todoForm.getCategoryId() != null) {
            categoryRepository.findById(todoForm.getCategoryId())
                .ifPresent(category -> model.addAttribute("category", category));
        }
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

    @PostMapping("/todos/{id}/update")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute TodoForm todoForm,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("todoForm", todoForm);
            model.addAttribute("categories", categoryRepository.findAll(Sort.by("name")));
            return "todo/form";
        }
        try {
            todoService.updateFromForm(id, todoForm);
        } catch (OptimisticLockingFailureException ex) {
            model.addAttribute("todoForm", todoForm);
            model.addAttribute("errorMessage", "他のユーザーにより更新されています。再読み込みしてください。");
            model.addAttribute("categories", categoryRepository.findAll(Sort.by("name")));
            return "todo/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "更新が完了しました");
        return "redirect:/todos";
    }

    // Toggle completion status.
    @PostMapping("/todos/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            todoService.toggleCompleted(id);
            redirectAttributes.addFlashAttribute("successMessage", "完了状態を更新しました");
        } catch (TodoNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "指定されたToDoが見つかりません");
        }
        return "redirect:/todos";
    }

    // Delete a todo by id.
    @PostMapping("/todos/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            todoService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "ToDoを削除しました");
        } catch (TodoNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました");
        }
        return "redirect:/todos";
    }

    @DeleteMapping("/todos/{id}")
    public String deleteById(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            todoService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "ToDoを削除しました");
        } catch (TodoNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました");
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
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("todoForm", todoService.getFormForEdit(id));
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("name")));
        return "todo/form";
    }
}
