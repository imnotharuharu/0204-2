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

    // Deadline is today or earlier.
    List<Todo> findByDeadlineLessThanEqual(LocalDate date);

    // Sort by priority (ascending).
    List<Todo> findAllByOrderByPriorityAsc();

    // Title partial match (case-insensitive) with createdAt desc.
    List<Todo> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);

    // Title partial match (case-insensitive) with Sort.
    List<Todo> findByTitleContainingIgnoreCase(String keyword, Sort sort);

    // Title partial match (case-insensitive) with Pageable.
    Page<Todo> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Todo> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Todo> findByTitleContainingIgnoreCaseAndCategoryId(String keyword, Long categoryId, Pageable pageable);

    Page<Todo> findByUserId(Long userId, Pageable pageable);

    Page<Todo> findByUserIdAndTitleContainingIgnoreCase(Long userId, String keyword, Pageable pageable);

    Page<Todo> findByUserIdAndCategoryId(Long userId, Long categoryId, Pageable pageable);

    Page<Todo> findByUserIdAndTitleContainingIgnoreCaseAndCategoryId(Long userId, String keyword, Long categoryId, Pageable pageable);

    java.util.Optional<Todo> findByIdAndUserId(Long id, Long userId);

    java.util.List<Todo> findAllByUserId(Long userId, Sort sort);

    java.util.List<Todo> findByIdInAndUserId(java.util.List<Long> ids, Long userId);

    // Example @Query: title partial match using JPQL.
    @Query("SELECT t FROM Todo t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Todo> searchByTitle(@Param("keyword") String keyword);

    // Example @Query: deadline is today or earlier.
    @Query("SELECT t FROM Todo t WHERE t.deadline <= :date")
    List<Todo> findOverdue(@Param("date") LocalDate date);
}
