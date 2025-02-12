package com.feddoubt.model.pojos;

import lombok.Data;

@Data
public class DownloadFileDetails {
    private String filename;
    private String path;
    private String mimeType;
}
