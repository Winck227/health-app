package com.healthapp.service;

import com.healthapp.common.BusinessException;
import com.healthapp.dto.FavoriteRequest;
import com.healthapp.entity.ArticleFavorite;
import com.healthapp.mapper.ArticleFavoriteMapper;
import com.healthapp.security.UserContext;
import com.healthapp.util.DateTimeUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ArticleFavoriteService {
    private final ArticleFavoriteMapper favoriteMapper;

    public ArticleFavoriteService(ArticleFavoriteMapper favoriteMapper) {
        this.favoriteMapper = favoriteMapper;
    }

    // 收藏列表按当前用户读取，前端资料页和管理端兼容接口都复用这份 DTO。
    public List<Map<String, Object>> list() {
        return favoriteMapper.listByUser(UserContext.getUserId()).stream().map(this::toDto).toList();
    }

    // 同一用户对同一文章只保留一条收藏，重复收藏时直接返回现有记录。
    public Map<String, Object> create(FavoriteRequest request) {
        Long userId = UserContext.getUserId();
        if (isBlank(request.getArticleUrl())) throw new BusinessException("文章链接不能为空");
        if (isBlank(request.getArticleTitle())) throw new BusinessException("文章标题不能为空");
        ArticleFavorite existing = favoriteMapper.findByUserAndUrl(userId, request.getArticleUrl().trim());
        if (existing != null) return toDto(existing);
        ArticleFavorite favorite = new ArticleFavorite();
        favorite.setUserId(userId);
        favorite.setArticleTitle(request.getArticleTitle().trim());
        favorite.setArticleUrl(request.getArticleUrl().trim());
        favorite.setSource(request.getSource());
        favorite.setSummary(request.getSummary());
        favorite.setFavoriteTime(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        return toDto(favorite);
    }

    // 删除收藏仍然带 userId 约束，避免通过枚举 id 删除他人的收藏记录。
    public Map<String, Object> delete(Long id) {
        int count = favoriteMapper.deleteByIdAndUser(id, UserContext.getUserId());
        if (count == 0) throw new BusinessException(404, "收藏不存在");
        return Map.of("success", true);
    }

    // 收藏 DTO 同时保留 camelCase 和下划线字段，兼容旧页面直接读取。
    public Map<String, Object> toDto(ArticleFavorite favorite) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", favorite.getId());
        map.put("userId", favorite.getUserId());
        map.put("user_id", favorite.getUserId());
        map.put("articleTitle", favorite.getArticleTitle());
        map.put("article_title", favorite.getArticleTitle());
        map.put("articleUrl", favorite.getArticleUrl());
        map.put("article_url", favorite.getArticleUrl());
        map.put("source", favorite.getSource());
        map.put("summary", favorite.getSummary());
        map.put("favoriteTime", DateTimeUtil.format(favorite.getFavoriteTime()));
        return map;
    }

    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
