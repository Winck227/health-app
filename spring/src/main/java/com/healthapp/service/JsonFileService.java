package com.healthapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class JsonFileService {
    // 服务说明：负责读取和写入后端配置文件，供食物库和计划模板管理使用。
    private final ObjectMapper objectMapper;
    private final Path dataDir;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public JsonFileService(ObjectMapper objectMapper, @Value("${app.data-dir:data}") String dataDir) {
        this.objectMapper = objectMapper;
        this.dataDir = Path.of(dataDir);
    }

    public Map<String, Object> readObject(String fileName) {
        try {
            Path path = resolve(fileName);
            if (!Files.exists(path)) throw new BusinessException(404, "配置文件不存在：" + fileName);
            return objectMapper.readValue(path.toFile(), new TypeReference<>() {});
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(500, "读取配置文件失败：" + fileName);
        }
    }

    public List<Map<String, Object>> readList(String fileName) {
        try {
            Path path = resolve(fileName);
            if (!Files.exists(path)) throw new BusinessException(404, "配置文件不存在：" + fileName);
            return objectMapper.readValue(path.toFile(), new TypeReference<>() {});
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(500, "读取配置文件失败：" + fileName);
        }
    }

    public Map<String, Object> version(String fileName) {
        try {
            Path path = resolve(fileName);
            if (!Files.exists(path)) throw new BusinessException(404, "配置文件不存在：" + fileName);
            long modified = Files.getLastModifiedTime(path).toMillis();
            String updatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(modified), ZoneId.systemDefault()).format(FORMATTER);
            return Map.of("version", String.valueOf(modified), "updatedAt", updatedAt);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(500, "读取文件版本失败：" + fileName);
        }
    }

    private Path resolve(String fileName) {
        Path path = dataDir.resolve(fileName).normalize();
        if (!path.startsWith(dataDir.normalize())) throw new BusinessException(400, "非法文件路径");
        return path;
    }
}
