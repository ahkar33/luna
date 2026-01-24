package com.luna.activity.entity;

import com.luna.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities", indexes = {
    @Index(name = "idx_activity_user_id", columnList = "user_id"),
    @Index(name = "idx_activity_type", columnList = "activity_type"),
    @Index(name = "idx_activity_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;
    
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // POST, COMMENT, etc.
    
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    
    @Column(name = "target_user_id")
    private Long targetUserId; // User who owns the post/comment that was liked
    
    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON for additional data
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
