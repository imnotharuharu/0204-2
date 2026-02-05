package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Todo;
import com.example.demo.form.TodoForm;
import com.example.demo.service.exception.TodoNotFoundException;
import com.example.demo.repository.TodoRepository;

@Service
public class TodoService {

    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    public Todo createFromForm(TodoForm form) {
        Todo todo = new Todo();
        todo.setTitle(form.getTitle());
        todo.setDescription(form.getDetail());
        todo.setPriority(form.getPriority() != null ? form.getPriority() : 1);
        todo.setCompleted(false);
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
        form.setDetail(todo.getDescription());
        form.setPriority(todo.getPriority());
        return form;
    }

    public void updateFromForm(Long id, TodoForm form) {
        Todo todo = todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        todo.setTitle(form.getTitle());
        todo.setDescription(form.getDetail());
        todo.setPriority(form.getPriority() != null ? form.getPriority() : 1);
        // optimistic lock: set version from the form
        todo.setVersion(form.getVersion());
        todoRepository.save(todo);
    }

    public void toggleCompleted(Long id) {
        Todo todo = todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        todo.setCompleted(!Boolean.TRUE.equals(todo.getCompleted()));
        todoRepository.save(todo);
    }
}
