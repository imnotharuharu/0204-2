package com.example.demo;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.Priority;
import com.example.demo.entity.Todo;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.TodoAttachmentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.TodoService;

import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TodoAttachmentRepository todoAttachmentRepository;

    @MockBean
    private FileStorageService fileStorageService;

    @BeforeEach
    void setupUser() {
        AppUser user = AppUser.builder().id(1L).username("haruka").role("USER").enabled(true).build();
        when(userRepository.findByUsername("haruka")).thenReturn(java.util.Optional.of(user));
        when(categoryRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(Collections.emptyList());
    }

    @Test
    @WithMockUser(username = "haruka", roles = "USER")
    @DisplayName("GET /todos returns list view")
    void list_returnsView() throws Exception {
        Page<Todo> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(todoService.findAll(any(org.springframework.data.domain.Pageable.class), eq(null), eq(1L), eq(false)))
            .thenReturn(emptyPage);

        mockMvc.perform(get("/todos"))
            .andExpect(status().isOk())
            .andExpect(view().name("todo/list"))
            .andExpect(model().attributeExists("todos"))
            .andExpect(model().attribute("todos", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "haruka", roles = "USER")
    @DisplayName("POST /todos/confirm with valid form returns confirm view")
    void confirm_validForm_returnsConfirmView() throws Exception {
        mockMvc.perform(post("/todos/confirm")
                .with(csrf())
                .param("author", "author")
                .param("title", "title")
                .param("detail", "detail")
                .param("priority", Priority.MEDIUM.name()))
            .andExpect(status().isOk())
            .andExpect(view().name("todo/confirm"))
            .andExpect(model().attributeExists("todoForm"));
    }
}
