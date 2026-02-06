package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.aop.Auditable;
import com.example.demo.entity.Todo;
import com.example.demo.entity.Priority;
import com.example.demo.entity.Category;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.TodoHistory;
import com.example.demo.form.TodoForm;
import com.example.demo.service.exception.TodoNotFoundException;
import com.example.demo.service.exception.TodoAccessDeniedException;
import com.example.demo.service.exception.CategoryNotFoundException;
import com.example.demo.service.exception.BusinessException;
import com.example.demo.repository.TodoRepository;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.TodoHistoryRepository;

@Service
public class TodoService {

    private final TodoRepository todoRepository;
    private final CategoryRepository categoryRepository;
    private final TodoHistoryRepository todoHistoryRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public TodoService(
        TodoRepository todoRepository,
        CategoryRepository categoryRepository,
        TodoHistoryRepository todoHistoryRepository,
        AuditLogService auditLogService,
        NotificationService notificationService
    ) {
        this.todoRepository = todoRepository;
        this.categoryRepository = categoryRepository;
        this.todoHistoryRepository = todoHistoryRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Auditable(action = "CREATE", entityType = "Todo", useResultEntityId = true)
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public Todo createFromForm(TodoForm form, AppUser user) {
        try {
            Todo todo = new Todo();
            todo.setTitle(form.getTitle());
            todo.setAuthor(form.getAuthor());
            todo.setDescription(form.getDetail());
            todo.setPriority(form.getPriority() != null ? form.getPriority() : Priority.MEDIUM);
            todo.setCompleted(false);
            todo.setDeadline(form.getDeadline());
            todo.setCategory(resolveCategory(form.getCategoryId()));
            todo.setUser(user);
            Todo saved = todoRepository.save(todo);
            todoHistoryRepository.save(TodoHistory.create(saved.getId(), "CREATE"));
            auditLogService.log("CREATE", "Todo created id=" + saved.getId());
            notificationService.sendTodoCreatedEmail(user.getUsername(), saved.getId());
            return saved;
        } catch (Exception ex) {
            auditLogService.log("CREATE_FAIL", ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<Todo> findAllByUserId(Long userId, Sort sort) {
        return todoRepository.findAllByUserId(userId, sort);
    }

    @Transactional(readOnly = true)
    public List<Todo> findAll(Sort sort) {
        return todoRepository.findAll(sort);
    }

    @Transactional(readOnly = true)
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

    @Auditable(action = "UPDATE", entityType = "Todo", entityIdParamIndex = 0)
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public void updateFromForm(Long id, TodoForm form, Long userId, boolean isAdmin) {
        try {
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
            todoHistoryRepository.save(TodoHistory.create(todo.getId(), "UPDATE"));
            auditLogService.log("UPDATE", "Todo updated id=" + todo.getId());
        } catch (Exception ex) {
            auditLogService.log("UPDATE_FAIL", ex.getMessage());
            throw ex;
        }
    }

    @Auditable(action = "UPDATE_API", entityType = "Todo", entityIdParamIndex = 0)
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public Todo updateFromApi(Long id, String title, String author, String detail, Priority priority, java.time.LocalDate deadline, Long categoryId, Boolean completed, Long userId, boolean isAdmin) {
        try {
            Todo todo = getOwnedTodo(id, userId, isAdmin);
            todo.setTitle(title);
            todo.setAuthor(author);
            todo.setDescription(detail);
            todo.setPriority(priority != null ? priority : Priority.MEDIUM);
            todo.setDeadline(deadline);
            todo.setCategory(resolveCategory(categoryId));
            if (completed != null) {
                todo.setCompleted(completed);
            }
            Todo saved = todoRepository.save(todo);
            todoHistoryRepository.save(TodoHistory.create(todo.getId(), "UPDATE_API"));
            auditLogService.log("UPDATE_API", "Todo updated id=" + todo.getId());
            return saved;
        } catch (Exception ex) {
            auditLogService.log("UPDATE_API_FAIL", ex.getMessage());
            throw ex;
        }
    }

    @Auditable(action = "DELETE", entityType = "Todo", entityIdParamIndex = 0)
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public void deleteById(Long id, Long userId, boolean isAdmin) {
        try {
            Todo todo = getOwnedTodo(id, userId, isAdmin);
            todoHistoryRepository.save(TodoHistory.create(todo.getId(), "DELETE"));
            todoRepository.deleteById(todo.getId());
            auditLogService.log("DELETE", "Todo deleted id=" + todo.getId());
        } catch (Exception ex) {
            auditLogService.log("DELETE_FAIL", ex.getMessage());
            throw ex;
        }
    }

    @Auditable(action = "TOGGLE", entityType = "Todo", entityIdParamIndex = 0)
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public void toggleCompleted(Long id, Long userId, boolean isAdmin) {
        try {
            Todo todo = getOwnedTodo(id, userId, isAdmin);
            todo.setCompleted(!Boolean.TRUE.equals(todo.getCompleted()));
            todoRepository.save(todo);
            todoHistoryRepository.save(TodoHistory.create(todo.getId(), "TOGGLE"));
            auditLogService.log("TOGGLE", "Todo toggled id=" + todo.getId());
        } catch (Exception ex) {
            auditLogService.log("TOGGLE_FAIL", ex.getMessage());
            throw ex;
        }
    }

    @Auditable(action = "BULK_DELETE", entityType = "Todo")
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public void deleteAllByIds(List<Long> ids, Long userId, boolean isAdmin) {
        try {
            if (isAdmin) {
                todoRepository.deleteAllByIdInBatch(ids);
                auditLogService.log("BULK_DELETE", "Bulk delete ids=" + ids);
                return;
            }
            List<Todo> owned = todoRepository.findByIdInAndUserId(ids, userId);
            todoRepository.deleteAllInBatch(owned);
            auditLogService.log("BULK_DELETE", "Bulk delete ids=" + ids);
        } catch (Exception ex) {
            auditLogService.log("BULK_DELETE_FAIL", ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
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
