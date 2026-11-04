package com.admaterial.util;

import com.admaterial.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 文件上传工具类
 * —— 课程 demo 用本地存储，不做对象存储接入
 */
@Component
public class FileUploadUtil {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${admaterial.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${admaterial.upload.max-size-mb:20}")
    private int maxSizeMb;

    @Value("${admaterial.upload.allowed-extensions:png,jpg,jpeg,pdf,ai,psd,webp,gif}")
    private String allowedExtensions;

    /**
     * 保存文件并返回可访问的 URL 路径
     *
     * @param file     上传的文件
     * @param subDir   业务子目录（如 "versions" 或 "task-123"）
     * @return 保存后的相对 URL（如 /uploads/versions/20261104/uuid.png）
     */
    public UploadResult save(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.unprocessable("上传文件不能为空");
        }

        // 1. 大小校验
        long maxBytes = (long) maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw BusinessException.unprocessable("文件大小超出上限（" + maxSizeMb + "MB）");
        }

        // 2. 扩展名校验
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw BusinessException.unprocessable("文件名无效");
        }
        String ext = getExtension(originalFilename).toLowerCase();
        Set<String> allowed = Set.of(allowedExtensions.split(","));
        if (!allowed.contains(ext)) {
            throw BusinessException.unprocessable(
                    "不支持的文件类型：" + ext + "，允许：" + allowedExtensions);
        }

        // 3. 构建保存路径
        String datePath = LocalDate.now().format(DATE_FMT);
        File targetDir = new File(uploadDir, subDir + "/" + datePath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new BusinessException(500, "创建上传目录失败");
        }

        // 4. 生成唯一文件名（UUID 避免重名覆盖）
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        String newFilename = uuid + "." + ext;
        File targetFile = new File(targetDir, newFilename);

        // 5. 保存
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败：" + e.getMessage());
        }

        // 6. 返回访问路径（静态资源映射后前端可直接访问）
        String fileUrl = "/uploads/" + subDir + "/" + datePath + "/" + newFilename;
        return new UploadResult(fileUrl, originalFilename, file.getSize(), targetFile.getAbsolutePath());
    }

    /** 从文件名取扩展名 */
    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1);
    }

    /** 文件上传结果 */
    public record UploadResult(String fileUrl, String originalFilename, long fileSizeBytes, String absolutePath) {}
}
