package com.feddoubt.YT1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@RefreshScope //实时刷新 Nacos 中更新的配置
@Component
public class ConfigProperties {

    @Value("${vagrant.id}")
    private String vagrantId;

    @Value("${ytdlp.path}")
    private String ytdlpPath;

    @Value("${ytdlp.contain-name}")
    private String ytdlpContainName;

    @Value("${ffmpeg.contain-name}")
    private String ffmpegContainName;

    @Value("${yt1.base-dir}")
    private String yt1BaseDir;


    public String getVagrantId() {
        return vagrantId;
    }

    public String getYtdlpPath() {
        return ytdlpPath;
    }

    public String getYtdlpContainName() {
        return ytdlpContainName;
    }

    public String getFfmpegContainName() {
        return ffmpegContainName;
    }

    public String getYt1BaseDir() {
        return yt1BaseDir;
    }
}
