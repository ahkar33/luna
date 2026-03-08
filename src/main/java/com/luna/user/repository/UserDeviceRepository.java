package com.luna.user.repository;

import com.luna.user.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {
    Optional<UserDevice> findByUserIdAndDeviceFingerprint(UUID userId, String deviceFingerprint);
    long countByUserIdAndVerified(UUID userId, Boolean verified);
}
