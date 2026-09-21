package com.company.ai.marketplace.controller;

import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.entity.MpSubmission;
import com.company.ai.marketplace.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 排行榜接口。
 */
@RestController
@RequestMapping("/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    /** 总榜 Top N（默认 50，前端可指定） */
    @GetMapping("/overall")
    public Result<List<MpSubmission>> overall(@RequestParam(defaultValue = "50") int n) {
        return Result.ok(rankingService.overallTopN(n));
    }

    /** 按赛道取 Top N */
    @GetMapping("/tracks/{track}")
    public Result<List<MpSubmission>> byTrack(@PathVariable String track,
                                              @RequestParam(defaultValue = "10") int n) {
        return Result.ok(rankingService.topNByTrack(track, n));
    }

    /** 按当前大赛的 topNPerTrack 聚合各赛道榜单 + 总榜 */
    @GetMapping("/competition")
    public Result<Map<String, List<MpSubmission>>> byCompetition() {
        return Result.ok(rankingService.byCurrentCompetition());
    }
}
