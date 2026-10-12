package com.company.ai.marketplace.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ai.marketplace.entity.SysUser;
import com.company.ai.marketplace.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时把 admin 账号从 {@code schema.sql} 的占位哈希里救出来。
 * <p>占位哈希不是任何实现的合法 BCrypt 输出，校验必然失败，admin 会永远登不进去，
 * 所以首次部署时需要重生成一次。
 * <p><b>只在哈希仍等于占位字面量时才动它。</b> 早期版本每次启动都拿硬编码默认口令去校验、
 * 不匹配就覆盖，等于运维改过的口令一重启就被打回，改密形同无效
 * （2026-10-08 与 2026-10-12 各发生一次）。现在运维设过的口令一律保留。
 * <p>重生成时取 {@code app.auth.default-password}，与管理员建号用的是同一个口径，
 * 不再另有一份硬编码值。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String ADMIN_ACCOUNT = "admin";

    /** schema.sql 中 admin 的 password_hash 占位值 */
    private static final String PLACEHOLDER_HASH =
            "$2a$10$placeholder.will.be.regenerated.by.DataInitializer.on.startup";

    private final SysUserMapper userMapper;
    private final AppProperties props;

    @Override
    public void run(String... args) {
        SysUser admin = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, ADMIN_ACCOUNT));
        if (admin == null) {
            log.warn("admin 账号不存在，跳过密码校验");
            return;
        }

        String hash = admin.getPasswordHash();
        boolean untouched = StrUtil.isBlank(hash) || PLACEHOLDER_HASH.equals(hash);
        if (!untouched) {
            log.info("admin 口令已由运维设置，本次启动不改动");
            return;
        }

        admin.setPasswordHash(BCrypt.hashpw(props.getAuth().getDefaultPassword()));
        admin.setMustChangePassword(1);
        userMapper.updateById(admin);
        log.info("admin 仍是 schema.sql 的占位哈希，已按 app.auth.default-password 重生成并置 must_change_password=1");
    }
}
