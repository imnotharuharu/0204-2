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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.demo.entity.Todo;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.TodoAttachment;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.TodoAttachmentRepository;
import com.example.demo.form.TodoForm;
import com.example.demo.service.TodoService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.exception.TodoNotFoundException;

@Controller
public class TodoController {

    private final TodoService todoService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TodoAttachmentRepository todoAttachmentRepository;
    private final FileStorageService fileStorageService;

    public TodoController(
        TodoService todoService,
        CategoryRepository categoryRepository,
        UserRepository userRepository,
        TodoAttachmentRepository todoAttachmentRepository,
        FileStorageService fileStorageService
    ) {
        this.todoService = todoService;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.todoAttachmentRepository = todoAttachmentRepository;
        this.fileStorageService = fileStorageService;
    }

    // Display the list of todos.
    @GetMapping("/todos")
    public String list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) String direction,
        @RequestParam(required = false) Long categoryId,
        @PageableDefault(size = 10) Pageable pageable,
        @AuthenticationPrincipal UserDetails userDetails,
        Model model
    ) {
        AppUser user = getCurrentUser(userDetails);
        Long userId = user.getId();
        boolean isAdmin = isAdmin(user);

        String sortKey = resolveSortKey(sort);
        Sort.Direction dir = resolveDirection(direction);
        Sort sortSpec = Sort.by(dir, sortKey);
        Pageable request = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortSpec);
        Page<Todo> page;

        if (keyword != null && !keyword.isBlank()) {
            page = todoService.searchByTitle(keyword, request, categoryId, userId, isAdmin);
        } else {
            page = todoService.findAll(request, categoryId, userId, isAdmin);
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
        if ("title".equals(sort) || "completed".equals(sort) || "createdAt".equals(sort) || "priority".equals(sort) || "deadline".equals(sort)) {
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
    public String newTodo(@AuthenticationPrincipal UserDetails userDetails, Model model) {
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
    public String complete(
        @ModelAttribute TodoForm todoForm,
        @AuthenticationPrincipal UserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        AppUser user = getCurrentUser(userDetails);
        todoService.createFromForm(todoForm, user);
        redirectAttributes.addFlashAttribute("successMessage", "登録が完了しました");
        return "redirect:/todos";
    }

    @PostMapping("/todos/{id}/update")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute TodoForm todoForm,
        BindingResult bindingResult,
        @AuthenticationPrincipal UserDetails userDetails,
        RedirectAttributes redirectAttributes,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("todoForm", todoForm);
            model.addAttribute("categories", categoryRepository.findAll(Sort.by("name")));
            return "todo/form";
        }
        try {
            AppUser user = getCurrentUser(userDetails);
            todoService.updateFromForm(id, todoForm, user.getId(), isAdmin(user));
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
    public String toggle(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            AppUser user = getCurrentUser(userDetails);
            todoService.toggleCompleted(id, user.getId(), isAdmin(user));
            redirectAttributes.addFlashAttribute("successMessage", "完了状態を更新しました");
        } catch (TodoNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "指定されたToDoが見つかりません");
        }
        return "redirect:/todos";
    }

    // Delete a todo by id.
    @PostMapping("/todos/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            AppUser user = getCurrentUser(userDetails);
            todoService.deleteById(id, user.getId(), isAdmin(user));
            redirectAttributes.addFlashAttribute("successMessage", "ToDoを削除しました");
        } catch (TodoNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました");
        }
        return "redirect:/todos";
    }

    @DeleteMapping("/todos/{id}")
    public String deleteById(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            AppUser user = getCurrentUser(userDetails);
            todoService.deleteById(id, user.getId(), isAdmin(user));
            redirectAttributes.addFlashAttribute("successMessage", "ToDoを削除しました");
        } catch (TodoNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました");
        }
        return "redirect:/todos";
    }

    @PostMapping("/todos/bulk-delete")
    public String bulkDelete(@RequestParam(required = false) List<Long> ids, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        if (ids == null || ids.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除する項目が選択されていません");
            return "redirect:/todos";
        }
        AppUser user = getCurrentUser(userDetails);
        todoService.deleteAllByIds(ids, user.getId(), isAdmin(user));
        redirectAttributes.addFlashAttribute("successMessage", "選択したToDoを削除しました");
        return "redirect:/todos";
    }

    @GetMapping({"/todo/export", "/todos/export"})
    public ResponseEntity<byte[]> exportCsv(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = getCurrentUser(userDetails);
        List<Todo> todos = isAdmin(user)
            ? todoService.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            : todoService.findAllByUserId(user.getId(), Sort.by(Sort.Direction.DESC, "createdAt"));
        byte[] csv = buildCsv(todos);

        String filename = "todo_" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        return ResponseEntity.ok().headers(headers).body(csv);
    }

    @PostMapping("/todos/{id}/attachments")
    public String uploadAttachment(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal UserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        AppUser user = getCurrentUser(userDetails);
        Todo todo = todoService.getOwnedTodo(id, user.getId(), isAdmin(user));
        FileStorageService.StoredFile stored = fileStorageService.store(file);
        TodoAttachment attachment = TodoAttachment.builder()
            .todo(todo)
            .originalFilename(stored.originalFilename())
            .storedFilename(stored.storedFilename())
            .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
            .size(file.getSize())
            .build();
        todoAttachmentRepository.save(attachment);
        redirectAttributes.addFlashAttribute("successMessage", "ファイルをアップロードしました");
        return "redirect:/todos/" + id + "/edit";
    }

    @GetMapping("/todos/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(
        @PathVariable Long attachmentId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        AppUser user = getCurrentUser(userDetails);
        TodoAttachment attachment = todoAttachmentRepository.findById(attachmentId).orElseThrow();
        todoService.getOwnedTodo(attachment.getTodo().getId(), user.getId(), isAdmin(user));
        Resource resource = fileStorageService.loadAsResource(attachment.getStoredFilename());
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(attachment.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getOriginalFilename() + "\"")
            .body(resource);
    }

    @PostMapping("/todos/attachments/{attachmentId}/delete")
    public String deleteAttachment(
        @PathVariable Long attachmentId,
        @AuthenticationPrincipal UserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        AppUser user = getCurrentUser(userDetails);
        TodoAttachment attachment = todoAttachmentRepository.findById(attachmentId).orElseThrow();
        todoService.getOwnedTodo(attachment.getTodo().getId(), user.getId(), isAdmin(user));
        fileStorageService.delete(attachment.getStoredFilename());
        todoAttachmentRepository.delete(attachment);
        redirectAttributes.addFlashAttribute("successMessage", "添付ファイルを削除しました");
        return "redirect:/todos/" + attachment.getTodo().getId() + "/edit";
    }

    private byte[] buildCsv(List<Todo> todos) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,タイトル,登録者,ステータス,作成日\r\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        for (Todo t : todos) {
            String status = Boolean.TRUE.equals(t.getCompleted()) ? "完了" : "未完了";
            String createdAt = t.getCreatedAt() != null ? t.getCreatedAt().format(fmt) : "";
            sb.append(csv(t.getId() != null ? t.getId().toString() : "")).append(",");
            sb.append(csv(t.getTitle())).append(",");
            sb.append(csv(t.getAuthor())).append(",");
            sb.append(csv(status)).append(",");
            sb.append(csv(createdAt)).append("\r\n");
        }

        byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(body, 0, out, bom.length, body.length);
        return out;
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    // Display the details for a single todo by id.
    @GetMapping("/todos/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = getCurrentUser(userDetails);
        todoService.getOwnedTodo(id, user.getId(), isAdmin(user));
        return "todo/detail";
    }

    // Show the form for editing a todo by id.
    @GetMapping("/todos/{id}/edit")
    public String edit(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        AppUser user = getCurrentUser(userDetails);
        model.addAttribute("todoForm", todoService.getFormForEdit(id, user.getId(), isAdmin(user)));
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("name")));
        model.addAttribute("attachments", todoAttachmentRepository.findByTodoId(id));
        return "todo/form";
    }

    private AppUser getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private boolean isAdmin(AppUser user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }
}
