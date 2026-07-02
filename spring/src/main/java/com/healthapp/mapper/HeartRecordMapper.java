package com.healthapp.mapper;

import com.healthapp.entity.HeartRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface HeartRecordMapper {
    @Select("SELECT * FROM heart_record WHERE user_id = #{userId} ORDER BY record_time DESC, id DESC LIMIT 1")
    HeartRecord latest(Long userId);

    @Select("SELECT * FROM heart_record WHERE user_id = #{userId} ORDER BY record_time DESC, id DESC")
    List<HeartRecord> listByUser(Long userId);

    @Select("SELECT COUNT(*) FROM heart_record")
    long countAll();

    @Insert("INSERT INTO heart_record (user_id, heart_rate, measure_type, record_time) " +
        "VALUES (#{userId}, #{heartRate}, #{measureType}, #{recordTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HeartRecord record);
}
