package com.feddoubt.YT1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@RefreshScope //实时刷新 Nacos 中更新的配置
@Component
public class ConfigProperties {

    @Value("${ytdlp.path}")
    private String ytdlpPath;

    @Value("${yt1.base-dir}")
    private String yt1BaseDir;

    public String getYtdlpPath() {
        return ytdlpPath;
    }

    public String getYt1BaseDir() {
        return yt1BaseDir;
    }
}
