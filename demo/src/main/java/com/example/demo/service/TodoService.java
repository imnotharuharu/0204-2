package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.entity.Todo;
import com.example.demo.form.TodoForm;
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
        todo.setDescription(form.getDescription());
        todo.setPriority(form.getPriority() != null ? form.getPriority() : 1);
        todo.setCompleted(false);
        return todoRepository.save(todo);
    }
}
