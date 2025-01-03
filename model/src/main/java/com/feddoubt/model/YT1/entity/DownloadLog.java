package com.feddoubt.model.YT1.entity;

//import jakarta.persistence.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "download_logs")
public class DownloadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "format", nullable = false)
    private String format;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public DownloadLog() {}

    public DownloadLog(String ipAddress, String url, String format, String userAgent, LocalDateTime createdAt) {
        this.ipAddress = ipAddress;
        this.url = url;
        this.format = format;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }

    // Getters and Setters
}
