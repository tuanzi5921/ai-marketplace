package com.company.ai.marketplace.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.dto.SubmissionCreateDTO;
import com.company.ai.marketplace.entity.MpSubmission;
import com.company.ai.marketplace.entity.MpSubmissionArtifact;
import com.company.ai.marketplace.integration.storage.StorageService;
import com.company.ai.marketplace.security.LoginUser;
import com.company.ai.marketplace.security.ThreadLocalContext;
import com.company.ai.marketplace.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;

/**
 * 作品接口：提交 / 列表 / 详情 / 下载。
 */
@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final StorageService storageService;

    /** 提交作品（multipart：file + 表单 JSON） */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Long> submit(@ModelAttribute SubmissionCreateDTO dto,
                               @RequestPart(value = "file", required = false) MultipartFile file) {
        return Result.ok(submissionService.submit(dto, file));
    }

    /** 分页查询已发布作品 */
    @GetMapping
    public Result<Page<MpSubmission>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String domain) {
        return Result.ok(submissionService.listPublished(page, size, type, domain));
    }

    /** 我的提交（当前登录用户的所有作品，不限状态） */
    @GetMapping("/mine")
    public Result<Page<MpSubmission>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LoginUser current = ThreadLocalContext.get();
        return Result.ok(submissionService.listMine(current.getId(), page, size));
    }

    /** 作品详情 */
    @GetMapping("/{id}")
    public Result<MpSubmission> detail(@PathVariable Long id) {
        return Result.ok(submissionService.detail(id));
    }

    /** 下载作品最新版本 */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws MalformedURLException {
        MpSubmissionArtifact artifact = submissionService.download(id);
        Path file = storageService.resolve(artifact.getStoredPath());
        UrlResource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + artifact.getFileName() + "\"")
                .body(resource);
    }
}
