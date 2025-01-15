package com.feddoubt.YT1.service;

import com.feddoubt.YT1.repo.UserLogRepository;
import com.feddoubt.common.YT1.config.message.RabbitResponse;
import com.feddoubt.model.YT1.entity.UserLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class UserLogService {

    private final UserLogRepository userLogRepository;

    public UserLogService(UserLogRepository userLogRepository) {
        this.userLogRepository = userLogRepository;
    }

    public Long findByIpAddress(String ipAddredd){
        return userLogRepository.findByIpAddress(ipAddredd).orElse(null);
    }

    public void saveUserLog(UserLog userLog){
        userLogRepository.save(userLog);
    }
}
