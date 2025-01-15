package com.feddoubt.YT1.repo;

import com.feddoubt.model.YT1.entity.DownloadLog;
import com.feddoubt.model.YT1.entity.UserLog;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLogRepository extends JpaRepository<UserLog, Long> {
    @Query("SELECT id FROM UserLog ul WHERE ul.ipAddress = :ipAddress")
    Optional<Long> findByIpAddress(@Param("ipAddress") String ipAddress);
}