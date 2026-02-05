package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.TodoHistory;

public interface TodoHistoryRepository extends JpaRepository<TodoHistory, Long> {
}
