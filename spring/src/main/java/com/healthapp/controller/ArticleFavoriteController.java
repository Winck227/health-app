package com.healthapp.controller;

import com.healthapp.common.Result;
import com.healthapp.dto.FavoriteRequest;
import com.healthapp.service.ArticleFavoriteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/article-favorite")
public class ArticleFavoriteController {
    private final ArticleFavoriteService favoriteService;

    public ArticleFavoriteController(ArticleFavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(favoriteService.list());
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody FavoriteRequest request) {
        return Result.ok(favoriteService.create(request));
    }

    @DeleteMapping("/{id}")
    public Result<Map<String, Object>> delete(@PathVariable Long id) {
        return Result.ok(favoriteService.delete(id));
    }
}
