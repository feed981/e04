package com.feddoubt.model.YT1.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.io.Serializable;

@Entity
@Table(name = "download_logs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DownloadLog  implements Serializable {
    private static final long serialVersionUID = 1L;

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

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "ext", nullable = false)
    private String ext;

    @Column(name = "sanitized_title", nullable = false)
    private String sanitizedTitle;

}
