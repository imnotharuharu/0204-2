package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.Priority;
import com.example.demo.entity.Todo;
import com.example.demo.form.TodoForm;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.TodoHistoryRepository;
import com.example.demo.repository.TodoRepository;
import com.example.demo.service.exception.TodoAccessDeniedException;
import com.example.demo.service.exception.TodoNotFoundException;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TodoHistoryRepository todoHistoryRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TodoService todoService;

    @Test
    @DisplayName("createFromForm: saves todo and writes history/audit")
    void createFromForm_savesTodo() {
        AppUser user = AppUser.builder().id(1L).username("haruka").role("USER").enabled(true).build();
        TodoForm form = new TodoForm();
        form.setTitle("title");
        form.setAuthor("author");
        form.setDetail("detail");
        form.setPriority(Priority.MEDIUM);
        form.setDeadline(LocalDate.now());

        Todo saved = new Todo();
        saved.setId(10L);
        when(todoRepository.save(any(Todo.class))).thenReturn(saved);

        Todo result = todoService.createFromForm(form, user);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(todoRepository).save(any(Todo.class));
        verify(todoHistoryRepository).save(any());
        verify(auditLogService).log(eq("CREATE"), any());
    }

    @Test
    @DisplayName("getOwnedTodo: owner access ok")
    void getOwnedTodo_ownerOk() {
        AppUser owner = AppUser.builder().id(1L).build();
        Todo todo = new Todo();
        todo.setId(1L);
        todo.setUser(owner);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));

        Todo result = todoService.getOwnedTodo(1L, 1L, false);

        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("getOwnedTodo: access denied for non-owner")
    void getOwnedTodo_accessDenied() {
        AppUser owner = AppUser.builder().id(2L).build();
        Todo todo = new Todo();
        todo.setId(1L);
        todo.setUser(owner);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));

        assertThrows(TodoAccessDeniedException.class, () -> todoService.getOwnedTodo(1L, 1L, false));
    }

    @Test
    @DisplayName("deleteById: throws when not found")
    void deleteById_notFound() {
        when(todoRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(TodoNotFoundException.class, () -> todoService.deleteById(1L, 1L, true));
    }
}
