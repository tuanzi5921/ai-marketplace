package com.company.ai.marketplace.controller;

import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.dto.CommentCreateDTO;
import com.company.ai.marketplace.dto.CommentReportDTO;
import com.company.ai.marketplace.entity.MpComment;
import com.company.ai.marketplace.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论接口（先发后审 + 举报制）。
 */
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 发表评论 */
    @PostMapping
    public Result<Long> create(@RequestBody CommentCreateDTO dto) {
        return Result.ok(commentService.create(dto));
    }

    /** 查询作品的可见评论列表 */
    @GetMapping("/submissions/{submissionId}")
    public Result<List<MpComment>> listBySubmission(@PathVariable Long submissionId) {
        return Result.ok(commentService.listBySubmission(submissionId));
    }

    /** 举报评论 */
    @PostMapping("/report")
    public Result<Void> report(@RequestBody CommentReportDTO dto) {
        commentService.report(dto);
        return Result.ok();
    }

    /** 删除自己的评论 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteOwn(@PathVariable Long id) {
        commentService.deleteOwn(id);
        return Result.ok();
    }

    /** 运营隐藏评论 */
    @PostMapping("/{id}/hide")
    public Result<Void> hide(@PathVariable Long id) {
        commentService.hide(id);
        return Result.ok();
    }

    /** 运营恢复评论 */
    @PostMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        commentService.restore(id);
        return Result.ok();
    }
}
