package com.luna.user.repository;

import com.luna.user.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    Optional<UserDevice> findByUserIdAndDeviceFingerprint(Long userId, String deviceFingerprint);
    long countByUserIdAndVerified(Long userId, Boolean verified);
}
