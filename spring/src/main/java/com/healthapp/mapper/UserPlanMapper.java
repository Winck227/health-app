package com.healthapp.mapper;

import com.healthapp.entity.UserPlan;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserPlanMapper {
    @Select("SELECT * FROM user_plan WHERE user_id = #{userId} AND status <> 'deleted' ORDER BY created_at DESC, id DESC")
    List<UserPlan> listByUser(Long userId);

    @Select("SELECT COUNT(*) FROM user_plan WHERE status <> 'deleted'")
    long countAll();

    @Select("SELECT * FROM user_plan WHERE id = #{id} AND user_id = #{userId} AND status <> 'deleted'")
    UserPlan findByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    @Insert("INSERT INTO user_plan (user_id, title, category, target_days, goal_text, theme, progress, status, start_date, end_date) " +
        "VALUES (#{userId}, #{title}, #{category}, #{targetDays}, #{goalText}, #{theme}, #{progress}, #{status}, #{startDate}, #{endDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserPlan plan);

    @Update("UPDATE user_plan SET progress = #{progress}, status = #{status} WHERE id = #{id} AND user_id = #{userId}")
    int updateProgressAndStatus(@Param("id") Long id, @Param("userId") Long userId, @Param("progress") Integer progress, @Param("status") String status);

    @Update("UPDATE user_plan SET status = 'deleted' WHERE id = #{id} AND user_id = #{userId}")
    int softDelete(@Param("id") Long id, @Param("userId") Long userId);
}
