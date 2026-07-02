package com.healthapp.mapper;

import com.healthapp.entity.DietRecord;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DietRecordMapper {
    @Select("SELECT * FROM diet_record WHERE user_id = #{userId} ORDER BY record_time DESC, id DESC")
    List<DietRecord> listByUser(Long userId);

    @Select("SELECT * FROM diet_record WHERE user_id = #{userId} AND record_time >= #{start} AND record_time < #{end} ORDER BY record_time DESC, id DESC")
    List<DietRecord> listByUserBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(SUM(calories), 0) FROM diet_record WHERE user_id = #{userId} AND record_time >= #{start} AND record_time < #{end}")
    BigDecimal sumCaloriesBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM diet_record")
    long countAll();

    @Insert("INSERT INTO diet_record (user_id, meal_type, food_name, weight, calories, record_time) " +
        "VALUES (#{userId}, #{mealType}, #{foodName}, #{weight}, #{calories}, #{recordTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DietRecord record);

    @Delete("DELETE FROM diet_record WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);
}
