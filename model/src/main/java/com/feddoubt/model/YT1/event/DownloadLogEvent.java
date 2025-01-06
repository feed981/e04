package com.feddoubt.model.YT1.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
@Data
@AllArgsConstructor
public class DownloadLogEvent implements Serializable {
    //為了避免類的序列化版本問題，建議顯式聲明 serialVersionUID。
    private static final long serialVersionUID = 1L;

    private String ipAddress;
    private String url;
    private String format;
    private String userAgent;

    // Constructor, Getters, and Setters
}
