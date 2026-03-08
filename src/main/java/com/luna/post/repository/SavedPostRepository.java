package com.luna.post.repository;

import com.luna.post.entity.SavedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, UUID> {

    boolean existsByUserIdAndPostId(UUID userId, UUID postId);

    Optional<SavedPost> findByUserIdAndPostId(UUID userId, UUID postId);

    Page<SavedPost> findByUserIdOrderBySavedAtDesc(UUID userId, Pageable pageable);

    void deleteByUserIdAndPostId(UUID userId, UUID postId);
}
