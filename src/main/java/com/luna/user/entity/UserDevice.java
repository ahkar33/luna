package com.luna.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "user_devices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String deviceFingerprint;

    private String deviceName;
    private String ipAddress;
    private String userAgent;

    @Builder.Default
    @Column(nullable = false)
    private Boolean verified = false;

    @Column(nullable = false)
    private Instant firstSeenAt;

    private Instant lastSeenAt;
    private Instant verifiedAt;

    @PrePersist
    protected void onCreate() {
        firstSeenAt = Instant.now();
        lastSeenAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastSeenAt = Instant.now();
    }
}
