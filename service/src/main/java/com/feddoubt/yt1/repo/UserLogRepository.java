package com.feddoubt.YT1.repo;

import com.feddoubt.model.entity.UserLog;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLogRepository extends JpaRepository<UserLog, Long> {
    @Query("SELECT ul FROM UserLog ul WHERE ul.ipAddress = :ipAddress ORDER BY ul.createdAt DESC")
    Page<UserLog> findByIpAddress(@Param("ipAddress") String ipAddress , Pageable pageable);
}