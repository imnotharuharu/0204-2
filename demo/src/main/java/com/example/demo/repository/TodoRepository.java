package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.entity.Todo;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // Filter by completion status.
    List<Todo> findByCompleted(Boolean completed);

    // Partial match search by title (case-insensitive).
    List<Todo> findByTitleContainingIgnoreCase(String keyword);

    // Due date is today or earlier.
    List<Todo> findByDueDateLessThanEqual(LocalDate date);

    // Sort by priority (ascending).
    List<Todo> findAllByOrderByPriorityAsc();

    // Title partial match (case-insensitive) with createdAt desc.
    List<Todo> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);

    // Title partial match (case-insensitive) with Sort.
    List<Todo> findByTitleContainingIgnoreCase(String keyword, Sort sort);

    // Title partial match (case-insensitive) with Pageable.
    Page<Todo> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    // Example @Query: title partial match using JPQL.
    @Query("SELECT t FROM Todo t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Todo> searchByTitle(@Param("keyword") String keyword);

    // Example @Query: due date is today or earlier.
    @Query("SELECT t FROM Todo t WHERE t.dueDate <= :date")
    List<Todo> findOverdue(@Param("date") LocalDate date);
}
