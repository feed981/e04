package com.feddoubt.utils;

import com.feddoubt.model.YT1.dtos.YT1Dto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    private static final String YT1baseDir = "C:\\YT1\\download\\";

    public static Map<String, Object> downloadFileYT1(String filename) throws IOException {
        Map<String, Object> map = new HashMap<>(2);
        // 檔案名稱驗證： 確保檔案名稱中沒有目錄穿越的字元（例如 ../ 或 ..\）
        if (filename.contains("..")) {
            throw new IllegalArgumentException("檔案名稱不合法: " + filename);
        }

        // 解析並規範化檔案路徑
        Path filePath = Paths.get(YT1baseDir).resolve(filename).normalize();
        map.put("Path",String.valueOf(filePath));
        logger.info("filePath:{}",filePath);

        // 限制在基礎目錄內： 確保解析後的 filePath 仍位於基礎目錄內
        if (!filePath.startsWith(YT1baseDir)) {
            throw new SecurityException("偵測到未授權的存取嘗試。");
        }

        // 檢查檔案是否存在： 在提供檔案之前，檢查檔案是否存在且可讀
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new FileNotFoundException("檔案不存在或無法讀取: " + filePath);
        }

        String mimeType = "application/octet-stream"; // 默認二進制類型
        if (filename.endsWith(".mp3")) {
            mimeType = "audio/mpeg";
        } else if (filename.endsWith(".mp4")) {
            mimeType = "video/mp4";
        }
        map.put("mimeType",mimeType);
        logger.info("mimeType:{}",mimeType);

        return map;
    }
}
