package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.TodoAttachment;

public interface TodoAttachmentRepository extends JpaRepository<TodoAttachment, Long> {
    List<TodoAttachment> findByTodoId(Long todoId);
}
