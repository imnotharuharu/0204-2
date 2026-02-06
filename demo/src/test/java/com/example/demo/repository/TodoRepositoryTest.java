package com.example.demo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.Priority;
import com.example.demo.entity.Todo;

@DataJpaTest
@Transactional
class TodoRepositoryTest {

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("findByUserIdAndTitleContainingIgnoreCase returns matching todos")
    void findByUserIdAndTitleContainingIgnoreCase_returnsMatches() {
        AppUser user = userRepository.save(AppUser.builder()
            .username("haruka")
            .password("pw")
            .role("USER")
            .enabled(true)
            .build());

        Todo todo = Todo.builder()
            .title("Spring Boot Task")
            .author("haruka")
            .description("detail")
            .priority(Priority.MEDIUM)
            .deadline(LocalDate.now())
            .completed(false)
            .user(user)
            .build();

        todoRepository.save(todo);

        Page<Todo> page = todoRepository.findByUserIdAndTitleContainingIgnoreCase(
            user.getId(),
            "spring",
            PageRequest.of(0, 10)
        );

        assertEquals(1, page.getTotalElements());
        assertTrue(page.getContent().get(0).getTitle().contains("Spring"));
    }
}
