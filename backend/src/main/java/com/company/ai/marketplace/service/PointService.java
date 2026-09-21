package com.company.ai.marketplace.service;

import com.company.ai.marketplace.entity.MpPointTransaction;
import com.company.ai.marketplace.entity.SysUser;
import com.company.ai.marketplace.mapper.MpPointTransactionMapper;
import com.company.ai.marketplace.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 贡献积分服务：记录流水 + 更新用户余额。
 * <p>积分获取规则（v1）：
 * <ul>
 *   <li>SUBMIT：提交作品 +5</li>
 *   <li>DOWNLOAD_THRESHOLD：下载量达阈值 +N</li>
 *   <li>RATING_THRESHOLD：评分达阈值 +N</li>
 *   <li>ADMIN_ADJUST：运营手动调整</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final SysUserMapper userMapper;
    private final MpPointTransactionMapper pointMapper;

    @Transactional
    public void award(Long userId, int delta, String reason, Long refId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) return;
        int balance = (user.getPoints() == null ? 0 : user.getPoints()) + delta;

        // 更新用户余额
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getPoints, balance));

        // 记录流水
        MpPointTransaction tx = new MpPointTransaction();
        tx.setUserId(userId);
        tx.setDelta(delta);
        tx.setBalance(balance);
        tx.setReason(reason);
        tx.setRefId(refId);
        pointMapper.insert(tx);
        log.info("积分变动: user={} delta={} reason={} balance={}", userId, delta, reason, balance);
    }
}
