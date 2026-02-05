package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Todo;
import com.example.demo.entity.Priority;
import com.example.demo.entity.Category;
import com.example.demo.form.TodoForm;
import com.example.demo.service.exception.TodoNotFoundException;
import com.example.demo.service.exception.CategoryNotFoundException;
import com.example.demo.repository.TodoRepository;
import com.example.demo.repository.CategoryRepository;

@Service
public class TodoService {

    private final TodoRepository todoRepository;
    private final CategoryRepository categoryRepository;

    public TodoService(TodoRepository todoRepository, CategoryRepository categoryRepository) {
        this.todoRepository = todoRepository;
        this.categoryRepository = categoryRepository;
    }

    public Todo createFromForm(TodoForm form) {
        Todo todo = new Todo();
        todo.setTitle(form.getTitle());
        todo.setAuthor(form.getAuthor());
        todo.setDescription(form.getDetail());
        todo.setPriority(form.getPriority() != null ? form.getPriority() : Priority.MEDIUM);
        todo.setCompleted(false);
        todo.setDeadline(form.getDeadline());
        todo.setCategory(resolveCategory(form.getCategoryId()));
        return todoRepository.save(todo);
    }

    public List<Todo> findAllOrderByCreatedAtDesc() {
        return todoRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<Todo> searchByTitleOrderByCreatedAtDesc(String keyword) {
        return todoRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword);
    }

    public List<Todo> findAll(Sort sort) {
        return todoRepository.findAll(sort);
    }

    public List<Todo> searchByTitle(String keyword, Sort sort) {
        return todoRepository.findByTitleContainingIgnoreCase(keyword, sort);
    }

    public Page<Todo> findAll(Pageable pageable) {
        return todoRepository.findAll(pageable);
    }

    public Page<Todo> searchByTitle(String keyword, Pageable pageable) {
        return todoRepository.findByTitleContainingIgnoreCase(keyword, pageable);
    }

    public void deleteById(Long id) {
        if (!todoRepository.existsById(id)) {
            throw new TodoNotFoundException(id);
        }
        todoRepository.deleteById(id);
    }

    public TodoForm getFormForEdit(Long id) {
        Todo todo = todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        TodoForm form = new TodoForm();
        form.setId(todo.getId());
        form.setVersion(todo.getVersion());
        form.setTitle(todo.getTitle());
        form.setAuthor(todo.getAuthor());
        form.setDetail(todo.getDescription());
        form.setPriority(todo.getPriority());
        form.setDeadline(todo.getDeadline());
        form.setCategoryId(todo.getCategory() != null ? todo.getCategory().getId() : null);
        return form;
    }

    public void updateFromForm(Long id, TodoForm form) {
        Todo todo = todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        todo.setTitle(form.getTitle());
        todo.setAuthor(form.getAuthor());
        todo.setDescription(form.getDetail());
        todo.setPriority(form.getPriority() != null ? form.getPriority() : Priority.MEDIUM);
        todo.setDeadline(form.getDeadline());
        todo.setCategory(resolveCategory(form.getCategoryId()));
        // optimistic lock: set version from the form
        todo.setVersion(form.getVersion());
        todoRepository.save(todo);
    }

    public Page<Todo> findAll(Pageable pageable, Long categoryId) {
        if (categoryId == null) {
            return todoRepository.findAll(pageable);
        }
        return todoRepository.findByCategoryId(categoryId, pageable);
    }

    public Page<Todo> searchByTitle(String keyword, Pageable pageable, Long categoryId) {
        if (categoryId == null) {
            return todoRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        }
        return todoRepository.findByTitleContainingIgnoreCaseAndCategoryId(keyword, categoryId, pageable);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    public void toggleCompleted(Long id) {
        Todo todo = todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        todo.setCompleted(!Boolean.TRUE.equals(todo.getCompleted()));
        todoRepository.save(todo);
    }

    public void deleteAllByIds(List<Long> ids) {
        todoRepository.deleteAllByIdInBatch(ids);
    }
}
