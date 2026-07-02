package com.healthapp.service;

import com.healthapp.common.BusinessException;
import com.healthapp.entity.User;
import com.healthapp.mapper.UserMapper;
import com.healthapp.security.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfileAvatarService {
    // 头像上传只接受常见静态图片格式，避免把任意文件写入公开目录。
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    );
    private static final long MAX_SIZE = 5 * 1024 * 1024L;

    @Value("${app.upload.avatar-dir:./uploads/avatar}")
    private String avatarDir;

    private final UserMapper userMapper;

    public ProfileAvatarService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Transactional
    public Map<String, Object> uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "头像文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(413, "头像文件不能超过 5MB");
        }

        String extension = resolveExtension(file);
        Long userId = UserContext.getUserId();
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或登录已过期");
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            Path baseDir = Paths.get(avatarDir).toAbsolutePath().normalize();
            Path dir = baseDir.resolve("u_" + userId).resolve(dateDir).normalize();
            // 显式校验最终路径仍在头像根目录下，避免路径拼接被利用成目录穿越。
            if (!dir.startsWith(baseDir)) {
                throw new BusinessException(400, "头像存储路径不合法");
            }
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName).normalize();
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException(500, "头像保存失败：" + ex.getMessage());
        }

        String avatar = "/uploads/avatar/u_" + userId + "/" + dateDir + "/" + fileName;
        user.setAvatar(avatar);
        userMapper.updateProfile(user);
        return avatarPayload(avatar);
    }

    @Transactional
    public Map<String, Object> clearAvatar() {
        Long userId = UserContext.getUserId();
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或登录已过期");
        }
        user.setAvatar("");
        userMapper.updateProfile(user);
        return avatarPayload("");
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = String.valueOf(file.getContentType()).toLowerCase(Locale.ROOT).trim();
        String original = String.valueOf(file.getOriginalFilename()).toLowerCase(Locale.ROOT).trim();

        // 同时参考 MIME 和原始文件名，尽量兼容移动端上传时 content-type 不稳定的情况。
        if (contentType.contains("jpeg") || contentType.contains("jpg") || original.endsWith(".jpg") || original.endsWith(".jpeg")) {
            return ".jpg";
        }
        if (contentType.contains("png") || original.endsWith(".png")) {
            return ".png";
        }
        if (contentType.contains("webp") || original.endsWith(".webp")) {
            return ".webp";
        }
        if (ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return ".jpg";
        }
        throw new BusinessException(415, "仅支持 JPG、PNG、WEBP 图片");
    }

    private Map<String, Object> avatarPayload(String avatar) {
        String safeAvatar = avatar == null ? "" : avatar;
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        // 同时返回相对路径和绝对地址，兼容不同前端环境的头像展示逻辑。
        String avatarUrl = safeAvatar.isBlank() ? "" : base + safeAvatar;
        return Map.of("avatar", safeAvatar, "avatarUrl", avatarUrl);
    }
}
