package com.company.ai.marketplace.integration.storage;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * 文件存储抽象。
 * <p>适配多形态作品交付：
 * <ul>
 *   <li>源码 zip / 可执行包：走文件存储（store/load）</li>
 *   <li>SaaS 链接：仅元数据，不走本服务</li>
 *   <li>文档：可直接走 store，也可结构化入库</li>
 * </ul>
 * <p>生产建议：替换为 NAS / 对象存储实现（S3、OSS、COS、Minio）。
 */
public abstract class StorageService {

    /**
     * 存储上传文件，返回存储元信息。
     */
    public abstract StoredFile store(MultipartFile file);

    /**
     * 读取已存储文件为字节流，用于下载。
     */
    public abstract byte[] load(String storedPath);

    /**
     * 删除已存储文件（作品下架时调用）。
     */
    public abstract boolean delete(String storedPath);

    /**
     * 将存储相对路径解析为可读取的物理路径（下载时用于构造 Resource）。
     */
    public abstract Path resolve(String storedPath);

    /**
     * 存储结果元信息。
     */
    @Data
    public static class StoredFile {
        /** 原始文件名 */
        private String fileName;
        /** 存储路径（含相对路径，用于 load/delete） */
        private String storedPath;
        /** 文件大小（字节） */
        private Long sizeBytes;
        /** MIME 类型 */
        private String contentType;
        /** 文件 hash（SHA-256）用于去重 / 校验 */
        private String hash;
    }
}
