package com.company.ai.marketplace.controller;

import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 运营后台数据看板接口。
 * <p>路径与前端 {@code frontend-admin/src/api/dashboard.ts} 对应：
 * GET /admin/dashboard/stats
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 看板统计：作品总数 / 待审数 / 下载量 / 用户数 / 当前大赛 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(dashboardService.stats());
    }
}
