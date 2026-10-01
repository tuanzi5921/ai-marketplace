package com.company.ai.marketplace.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ai.marketplace.entity.SysUser;
import com.company.ai.marketplace.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时校验 / 修复 admin 账号密码哈希。
 * <p>schema.sql 中的 BCrypt hash 是占位值，不同 BCrypt 实现生成的 hash 不同。
 * 本 Runner 在启动时检查 admin 的 password_hash 是否能匹配 {@code Welcome@2026}，
 * 不能匹配则用当前运行时 BCrypt 实现重新生成。
 * <p>同时确保 admin 的 must_change_password=1（强制首次改密）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String ADMIN_ACCOUNT = "admin";
    private static final String DEFAULT_PASSWORD = "Welcome@2026";

    private final SysUserMapper userMapper;

    @Override
    public void run(String... args) {
        SysUser admin = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, ADMIN_ACCOUNT));
        if (admin == null) {
            log.warn("admin 账号不存在，跳过密码校验");
            return;
        }

        boolean needFix = false;
        String hash = admin.getPasswordHash();

        if (hash == null || hash.isBlank()) {
            needFix = true;
        } else {
            try {
                if (!BCrypt.checkpw(DEFAULT_PASSWORD, hash)) {
                    needFix = true;
                }
            } catch (Exception e) {
                log.warn("admin 密码哈希格式异常，将重新生成: {}", e.getMessage());
                needFix = true;
            }
        }

        if (needFix) {
            String newHash = BCrypt.hashpw(DEFAULT_PASSWORD);
            admin.setPasswordHash(newHash);
            log.info("admin 密码哈希已用运行时 BCrypt 重新生成");
        }

        if (admin.getMustChangePassword() == null || admin.getMustChangePassword() != 1) {
            admin.setMustChangePassword(1);
            log.info("admin must_change_password 已修正为 1");
        }

        if (needFix || admin.getMustChangePassword() == null || admin.getMustChangePassword() != 1) {
            userMapper.updateById(admin);
            log.info("admin 账号已修复：password_hash={}, must_change_password=1", needFix);
        }
    }
}
