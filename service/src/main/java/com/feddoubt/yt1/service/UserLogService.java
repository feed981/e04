package com.feddoubt.YT1.service;

import com.feddoubt.YT1.repo.UserLogRepository;
import com.feddoubt.model.entity.UserLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
