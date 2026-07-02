package com.healthapp.service;

import com.healthapp.common.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PlanTemplateService {
    private final JsonFileService jsonFileService;
    private static final String FILE = "plan-templates.json";

    public PlanTemplateService(JsonFileService jsonFileService) {
        this.jsonFileService = jsonFileService;
    }

    public Map<String, Object> version() {
        return jsonFileService.version(FILE);
    }

    public List<Map<String, Object>> templates() {
        return jsonFileService.readList(FILE).stream()
            .filter(item -> !Boolean.FALSE.equals(item.get("enabled")))
            .toList();
    }

    public Map<String, Object> findTemplate(String templateId) {
        if (templateId == null || templateId.isBlank()) throw new BusinessException("模板编号不能为空");
        return templates().stream()
            .filter(item -> templateId.equals(String.valueOf(item.get("id"))))
            .findFirst()
            .orElseThrow(() -> new BusinessException(404, "计划模板不存在"));
    }
}
