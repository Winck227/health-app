package com.healthapp.mapper;

import com.healthapp.entity.ArticleFavorite;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ArticleFavoriteMapper {
    @Select("SELECT * FROM article_favorite WHERE user_id = #{userId} ORDER BY favorite_time DESC, id DESC")
    List<ArticleFavorite> listByUser(Long userId);

    @Select("SELECT * FROM article_favorite WHERE user_id = #{userId} AND article_url = #{articleUrl} LIMIT 1")
    ArticleFavorite findByUserAndUrl(@Param("userId") Long userId, @Param("articleUrl") String articleUrl);

    @Insert("INSERT INTO article_favorite (user_id, article_title, article_url, source, summary, favorite_time) " +
        "VALUES (#{userId}, #{articleTitle}, #{articleUrl}, #{source}, #{summary}, #{favoriteTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ArticleFavorite favorite);

    @Delete("DELETE FROM article_favorite WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);
}
