package com.company.ai.marketplace.controller;

import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.dto.CompetitionDTO;
import com.company.ai.marketplace.entity.MpCompetition;
import com.company.ai.marketplace.service.CompetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 大赛配置接口（运营后台 / 超管）。
 * <p>权限由 CompetitionService 内部 requireAdmin() 强校验。
 */
@RestController
@RequestMapping("/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    /** 创建大赛（草稿） */
    @PostMapping
    public Result<Long> create(@RequestBody CompetitionDTO dto) {
        return Result.ok(competitionService.create(dto));
    }

    /** 更新大赛配置 */
    @PutMapping
    public Result<Void> update(@RequestBody CompetitionDTO dto) {
        competitionService.update(dto);
        return Result.ok();
    }

    /** 激活大赛：DRAFT → ACTIVE */
    @PostMapping("/{id}/activate")
    public Result<Void> activate(@PathVariable Long id) {
        competitionService.activate(id);
        return Result.ok();
    }

    /** 切换大赛阶段：SUBMIT → REVIEW → AWARD */
    @PostMapping("/{id}/phase")
    public Result<Void> switchPhase(@PathVariable Long id, @RequestParam String phase) {
        competitionService.switchPhase(id, phase);
        return Result.ok();
    }

    /** 获取当前进行中的大赛（前端倒计时使用） */
    @GetMapping("/current")
    public Result<MpCompetition> current() {
        return Result.ok(competitionService.current());
    }

    /** 大赛列表（运营后台） */
    @GetMapping
    public Result<List<MpCompetition>> list() {
        return Result.ok(competitionService.listAll());
    }

    /** 大赛详情 */
    @GetMapping("/{id}")
    public Result<MpCompetition> detail(@PathVariable Long id) {
        return Result.ok(competitionService.detail(id));
    }
}
