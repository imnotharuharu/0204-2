package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Todo;
import com.example.demo.entity.Priority;
import com.example.demo.entity.Category;
import com.example.demo.entity.AppUser;
import com.example.demo.form.TodoForm;
import com.example.demo.service.exception.TodoNotFoundException;
import com.example.demo.service.exception.TodoAccessDeniedException;
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

    public Todo createFromForm(TodoForm form, AppUser user) {
        Todo todo = new Todo();
        todo.setTitle(form.getTitle());
        todo.setAuthor(form.getAuthor());
        todo.setDescription(form.getDetail());
        todo.setPriority(form.getPriority() != null ? form.getPriority() : Priority.MEDIUM);
        todo.setCompleted(false);
        todo.setDeadline(form.getDeadline());
        todo.setCategory(resolveCategory(form.getCategoryId()));
        todo.setUser(user);
        return todoRepository.save(todo);
    }

    public Page<Todo> findAll(Pageable pageable, Long categoryId, Long userId, boolean isAdmin) {
        if (isAdmin) {
            if (categoryId == null) {
                return todoRepository.findAll(pageable);
            }
            return todoRepository.findByCategoryId(categoryId, pageable);
        }
        if (categoryId == null) {
            return todoRepository.findByUserId(userId, pageable);
        }
        return todoRepository.findByUserIdAndCategoryId(userId, categoryId, pageable);
    }

    public Page<Todo> searchByTitle(String keyword, Pageable pageable, Long categoryId, Long userId, boolean isAdmin) {
        if (isAdmin) {
            if (categoryId == null) {
                return todoRepository.findByTitleContainingIgnoreCase(keyword, pageable);
            }
            return todoRepository.findByTitleContainingIgnoreCaseAndCategoryId(keyword, categoryId, pageable);
        }
        if (categoryId == null) {
            return todoRepository.findByUserIdAndTitleContainingIgnoreCase(userId, keyword, pageable);
        }
        return todoRepository.findByUserIdAndTitleContainingIgnoreCaseAndCategoryId(userId, keyword, categoryId, pageable);
    }

    public List<Todo> findAllByUserId(Long userId, Sort sort) {
        return todoRepository.findAllByUserId(userId, sort);
    }

    public List<Todo> findAll(Sort sort) {
        return todoRepository.findAll(sort);
    }

    public TodoForm getFormForEdit(Long id, Long userId, boolean isAdmin) {
        Todo todo = getOwnedTodo(id, userId, isAdmin);
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

    public void updateFromForm(Long id, TodoForm form, Long userId, boolean isAdmin) {
        Todo todo = getOwnedTodo(id, userId, isAdmin);
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

    public void deleteById(Long id, Long userId, boolean isAdmin) {
        Todo todo = getOwnedTodo(id, userId, isAdmin);
        todoRepository.deleteById(todo.getId());
    }

    public void toggleCompleted(Long id, Long userId, boolean isAdmin) {
        Todo todo = getOwnedTodo(id, userId, isAdmin);
        todo.setCompleted(!Boolean.TRUE.equals(todo.getCompleted()));
        todoRepository.save(todo);
    }

    public void deleteAllByIds(List<Long> ids, Long userId, boolean isAdmin) {
        if (isAdmin) {
            todoRepository.deleteAllByIdInBatch(ids);
            return;
        }
        List<Todo> owned = todoRepository.findByIdInAndUserId(ids, userId);
        todoRepository.deleteAllInBatch(owned);
    }

    public Todo getOwnedTodo(Long id, Long userId, boolean isAdmin) {
        Todo todo = todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        if (!isAdmin && (todo.getUser() == null || !todo.getUser().getId().equals(userId))) {
            throw new TodoAccessDeniedException();
        }
        return todo;
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}
