package com.feddoubt.model.YT1.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DownloadLogEvent {
    private String ipAddress;
    private String url;
    private String format;
    private String userAgent;

    // Constructor, Getters, and Setters
}
