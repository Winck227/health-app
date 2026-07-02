package com.healthapp.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FoodService {
    private static final String FILE = "food-nutrition.json";
    private final JsonFileService jsonFileService;

    public FoodService(JsonFileService jsonFileService) {
        this.jsonFileService = jsonFileService;
    }

    public Map<String, Object> version() {
        return jsonFileService.version(FILE);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> foods() {
        Map<String, Object> data = jsonFileService.readObject(FILE);
        if (!data.containsKey("source")) {
            data.put("source", "backend-json");
        }
        Object rawFoods = data.get("foods");
        if (rawFoods instanceof List<?> list) {
            List<Map<String, Object>> enabledFoods = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> food = (Map<String, Object>) map;
                    if (!Boolean.FALSE.equals(food.get("enabled"))) {
                        enabledFoods.add(food);
                    }
                }
            }
            data.put("foods", enabledFoods);
        }
        return data;
    }
}
