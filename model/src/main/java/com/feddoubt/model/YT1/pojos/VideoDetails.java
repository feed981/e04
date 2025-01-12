package com.feddoubt.model.YT1.pojos;

import lombok.Data;

import java.io.Serializable;

@Data
public class VideoDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String sanitizedTitle;
    private String ext;
    private String format;
    private String message;
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

    public String setDownloadFilename() {
        return this.downloadFilename = this.title + this.ext;
    }

    public String setDownloadVideoPath(String downloadVideoPath) {
        return this.downloadVideoPath = downloadVideoPath;
    }

    public String setConvertFilename() {
        return this.convertFilename = this.title + "." + this.format;
    }

    public String setConvertVideoPath(String convertVideoPath) {
        return this.convertVideoPath = convertVideoPath;
    }
}
