package com.company.ai.marketplace.integration.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.company.ai.marketplace.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地磁盘存储实现（默认 fallback）。
 * <p>路径规则：{basePath}/{yyyyMMdd}/{uuid}{extName}
 * <p>生产部署请实现 NAS / 对象存储版本并标注 {@code @Primary} 覆盖本 Bean。
 * <p>注意：这里不能用 {@code @ConditionalOnMissingBean}——该注解只对 auto-configuration
 * 的 {@code @Bean} 方法可靠，放在 {@code @Component} 上会在扫描期把自身算作已存在的
 * StorageService 而跳过注册，导致容器内没有任何 StorageService 可注入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalStorageService extends StorageService {

    private final AppProperties appProperties;

    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        try {
            String basePath = appProperties.getStorage().getBasePath();
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path dir = Paths.get(basePath, dateDir);
            Files.createDirectories(dir);

            String originalName = file.getOriginalFilename();
            String extName = "";
            if (originalName != null && originalName.contains(".")) {
                extName = originalName.substring(originalName.lastIndexOf("."));
            }
            String storedName = IdUtil.fastSimpleUUID() + extName;
            Path target = dir.resolve(storedName);
            file.transferTo(target.toFile());

            StoredFile sf = new StoredFile();
            sf.setFileName(originalName);
            // storedPath 用相对路径，方便 load/delete 时拼接 basePath
            sf.setStoredPath(dateDir + "/" + storedName);
            sf.setSizeBytes(file.getSize());
            sf.setContentType(file.getContentType());
            sf.setHash(SecureUtil.sha256(target.toFile()));
            log.info("文件已存储: name={} size={} path={}", originalName, file.getSize(), sf.getStoredPath());
            return sf;
        } catch (IOException e) {
            log.error("文件存储失败", e);
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] load(String storedPath) {
        try {
            Path path = Paths.get(appProperties.getStorage().getBasePath(), storedPath);
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("文件读取失败 path={}", storedPath, e);
            throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String storedPath) {
        Path path = Paths.get(appProperties.getStorage().getBasePath(), storedPath);
        File f = path.toFile();
        return f.exists() && f.delete();
    }

    @Override
    public Path resolve(String storedPath) {
        return Paths.get(appProperties.getStorage().getBasePath(), storedPath);
    }
}
