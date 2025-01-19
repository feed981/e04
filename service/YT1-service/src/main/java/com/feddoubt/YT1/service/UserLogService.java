package com.feddoubt.YT1.service;

import com.feddoubt.YT1.repo.UserLogRepository;
import com.feddoubt.common.YT1.config.message.RabbitResponse;
import com.feddoubt.model.YT1.entity.UserLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public UserLog findByIpAddress(String ipAddress){
        Pageable pageable = PageRequest.of(0, 1);
        Page<UserLog> result = userLogRepository.findByIpAddress(ipAddress, pageable);
        return result.isEmpty() ? null : result.getContent().get(0);
    }

    public void saveUserLog(UserLog userLog){
        userLogRepository.save(userLog);
    }
}
