package com.feddoubt.YT1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Base64;

@RefreshScope //实时刷新 Nacos 中更新的配置
@Component
public class ConfigProperties {

    @Value("${vagrant.id}")
    private String vagrantId;

    @Value("${yt1.base-dir}")
    private String yt1BaseDir;

    public String getVagrantId() {
        return vagrantId;
    }

    public String getYt1BaseDir() {
        return yt1BaseDir;
    }

}
