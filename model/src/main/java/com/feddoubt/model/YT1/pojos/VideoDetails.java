package com.feddoubt.model.YT1.pojos;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class VideoDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
    private String videoId;
    private String title;
    private String sanitizedTitle;
    private String ext;
    private BigDecimal duration;
    private String format;
    private String message;
    private String errorMessage;
    private String downloadFilename;
    private String downloadVideoPath;
    private String convertFilename;
    private String convertVideoPath;

    public void setExt(String ext) {
        if (ext.contains("mp4")) {
            this.ext = "." + ext;
        } else {
            this.ext = ".mp4." + ext;
        }
    }

    public void setDownloadFilename() {
        this.downloadFilename = this.title + this.ext;
    }

    public void setConvertFilename() {
        this.convertFilename = this.title + "." + this.format;
    }
}
