package com.company.ai.marketplace.service;

import com.company.ai.marketplace.entity.SysAuditLog;
import com.company.ai.marketplace.mapper.SysAuditLogMapper;
import com.company.ai.marketplace.security.LoginUser;
import com.company.ai.marketplace.security.ThreadLocalContext;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务：4 类日志（SUBMISSION/REVIEW/PERMISSION/SENSITIVE）。
 * <p>异步写入，不阻塞主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final SysAuditLogMapper auditMapper;

    @Async
    public void log(String category, String action, String targetType, Long targetId, String detail) {
        SysAuditLog log = new SysAuditLog();
        log.setId(IdWorker.getId());
        log.setCategory(category);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        LoginUser current = ThreadLocalContext.get();
        log.setUserId(current != null ? current.getId() : null);
        auditMapper.insert(log);
    }
}
