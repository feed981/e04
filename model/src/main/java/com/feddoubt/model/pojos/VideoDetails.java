package com.feddoubt.model.pojos;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class VideoDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
    private String videoId;
    private String title;
    private String ext;
    private BigDecimal duration;
    private String format;
    private String message;
    private String errorMessage;
    private String path;

    public void setPath(String base) {
        this.path = base + title;
    }
}
