package com.feddoubt.model.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.io.Serializable;

@Entity
@Table(name = "download_logs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
// queue need Serializable
public class DownloadLog  implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserLog userLog;

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

    @Column
    private Long uid;

}
