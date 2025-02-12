package com.feddoubt.yt1.repo;

import com.feddoubt.model.entity.DownloadLog;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DownloadLogRepository extends JpaRepository<DownloadLog, Long> {
    @Query(value = "SELECT d.ext " +
            "FROM DownloadLog d " +
            "WHERE d.title = :title " +
            "AND d.ext LIKE CONCAT(:ext, '%')")
    Optional<String> findByTitleAndExt(@Param("title") String title ,@Param("ext") String ext);
}