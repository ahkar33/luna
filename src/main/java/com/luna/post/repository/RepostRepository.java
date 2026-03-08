package com.luna.post.repository;

import com.luna.post.entity.Repost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepostRepository extends JpaRepository<Repost, UUID> {

    boolean existsByUserIdAndOriginalPostId(UUID userId, UUID postId);

    Optional<Repost> findByUserIdAndOriginalPostId(UUID userId, UUID postId);

    Page<Repost> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    void deleteByUserIdAndOriginalPostId(UUID userId, UUID postId);

    long countByOriginalPostId(UUID postId);
}
