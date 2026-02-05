package com.example.demo.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.Todo;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.TodoService;
import com.example.demo.service.exception.TodoNotFoundException;
import com.example.demo.service.exception.TodoAccessDeniedException;

@RestController
@RequestMapping("/api/todos")
@CrossOrigin
public class TodoApiController {

    private final TodoService todoService;
    private final UserRepository userRepository;

    public TodoApiController(TodoService todoService, UserRepository userRepository) {
        this.todoService = todoService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TodoApiResponse>>> list(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = getCurrentUser(userDetails);
        boolean isAdmin = isAdmin(user);
        List<Todo> todos = isAdmin
            ? todoService.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
            : todoService.findAllByUserId(user.getId(), org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        List<TodoApiResponse> data = todos.stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("OK", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TodoApiResponse>> get(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            AppUser user = getCurrentUser(userDetails);
            Todo todo = todoService.getOwnedTodo(id, user.getId(), isAdmin(user));
            return ResponseEntity.ok(ApiResponse.ok("OK", toResponse(todo)));
        } catch (TodoNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not Found"));
        } catch (TodoAccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("Forbidden"));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TodoApiResponse>> create(
        @Valid @RequestBody TodoApiRequest request,
        BindingResult bindingResult,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Validation error"));
        }
        AppUser user = getCurrentUser(userDetails);
        Todo todo = todoService.createFromForm(toForm(request), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Created", toResponse(todo)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TodoApiResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody TodoApiRequest request,
        BindingResult bindingResult,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Validation error"));
        }
        try {
            AppUser user = getCurrentUser(userDetails);
            Todo todo = todoService.updateFromApi(
                id,
                request.getTitle(),
                request.getAuthor(),
                request.getDetail(),
                request.getPriority(),
                request.getDeadline(),
                request.getCategoryId(),
                request.getCompleted(),
                user.getId(),
                isAdmin(user)
            );
            return ResponseEntity.ok(ApiResponse.ok("Updated", toResponse(todo)));
        } catch (TodoNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not Found"));
        } catch (TodoAccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("Forbidden"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            AppUser user = getCurrentUser(userDetails);
            todoService.deleteById(id, user.getId(), isAdmin(user));
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.ok("Deleted", null));
        } catch (TodoNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not Found"));
        } catch (TodoAccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("Forbidden"));
        }
    }

    private TodoApiResponse toResponse(Todo todo) {
        TodoApiResponse res = new TodoApiResponse();
        res.setId(todo.getId());
        res.setTitle(todo.getTitle());
        res.setAuthor(todo.getAuthor());
        res.setDetail(todo.getDescription());
        res.setPriority(todo.getPriority());
        res.setDeadline(todo.getDeadline());
        res.setCompleted(todo.getCompleted());
        res.setCreatedAt(todo.getCreatedAt());
        if (todo.getCategory() != null) {
            res.setCategoryId(todo.getCategory().getId());
            res.setCategoryName(todo.getCategory().getName());
        }
        return res;
    }

    private com.example.demo.form.TodoForm toForm(TodoApiRequest request) {
        com.example.demo.form.TodoForm form = new com.example.demo.form.TodoForm();
        form.setTitle(request.getTitle());
        form.setAuthor(request.getAuthor());
        form.setDetail(request.getDetail());
        form.setPriority(request.getPriority());
        form.setDeadline(request.getDeadline());
        form.setCategoryId(request.getCategoryId());
        return form;
    }

    private AppUser getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private boolean isAdmin(AppUser user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }
}
