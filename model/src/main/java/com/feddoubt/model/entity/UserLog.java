package com.feddoubt.model.entity;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "user_logs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
// queue need Serializable
public class UserLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @OneToMany(mappedBy = "userLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DownloadLog> downloadLogs;

    @Column(name = "loc")
    private String loc;

//    @Column(name = "latitude")
//    private BigDecimal latitude; // 緯度 lat
//
//    @Column(name = "longitude")
//    private BigDecimal longitude; // 經度 lng

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column
    private Long uid;

//    @Column
//    private String method;
}
