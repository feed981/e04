package com.feddoubt.common.YT1.s3;

import org.springframework.stereotype.Service;


import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.nio.file.Paths;

//@Service
public class S3Service {

    private final S3Client s3Client;

    public S3Service() {
        this.s3Client = S3Client.builder().build();
    }

    private final String bucketName = "your-bucket-name";

    public void uploadFileToS3(String filePath) {
        String key = new File(filePath).getName(); // 取得文件名作為 S3 鍵
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(new File(filePath)));
    }
}
