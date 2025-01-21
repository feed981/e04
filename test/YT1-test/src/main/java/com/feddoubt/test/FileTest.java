package com.feddoubt.test;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.time.Duration;

public class FileTest {

    private static final String bucketName = "";
    private static final String key = "path/in/s3/yourfile.txt";

    public static void main(String[] args) {
        fileExist();
    }

    public static void fileExist(){
        String path = "D:\\local\\yt1\\download\\Gran Turismo 3 Soundtrack - Championship Award";
        boolean exists = new File(path + ".mp4").exists();
        System.out.println(exists);
        path = path + ".mp4";
        System.out.println(path);

    }
    public static void S3Uploader(){
        String filePath = "C:/YT1/download/yourfile.txt";

        // 初始化 S3 客戶端
        S3Client s3 = S3Client.builder()
                .credentialsProvider(ProfileCredentialsProvider.create()) // 預設使用 AWS CLI 配置的憑證
                .build();

        // 上傳檔案
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build(),
                new File(filePath).toPath()
        );

        System.out.println("File uploaded to S3: " + bucketName + "/" + key);
    }

    public static void S3DownloadLink () {
        // 初始化 S3 Presigner
        S3Presigner presigner = S3Presigner.create();

        // 生成簽名 URL
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(b -> b.bucket(bucketName).key(key))
                .signatureDuration(Duration.ofMinutes(30)) // 簽名有效期
                .build();

        String url = presigner.presignGetObject(presignRequest).url().toString();
        System.out.println("Download link: " + url);

        presigner.close();
    }
}
