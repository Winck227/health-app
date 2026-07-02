package com.healthapp.mapper;

import com.healthapp.entity.HealthRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface HealthRecordMapper {
    @Select("SELECT * FROM health_record WHERE user_id = #{userId} ORDER BY record_time DESC, id DESC LIMIT 1")
    HealthRecord latest(Long userId);

    @Select("SELECT * FROM health_record WHERE user_id = #{userId} ORDER BY record_time DESC, id DESC")
    List<HealthRecord> listByUser(Long userId);

    @Select("SELECT COUNT(*) FROM health_record")
    long countAll();

    @Insert("INSERT INTO health_record (user_id, height, weight, bmi, bmr, record_time) " +
        "VALUES (#{userId}, #{height}, #{weight}, #{bmi}, #{bmr}, #{recordTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HealthRecord record);
}
