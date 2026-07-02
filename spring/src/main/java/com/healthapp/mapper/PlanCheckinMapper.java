package com.healthapp.mapper;

import com.healthapp.entity.PlanCheckin;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PlanCheckinMapper {
    @Select("SELECT checkin_date FROM plan_checkin WHERE plan_id = #{planId} AND user_id = #{userId} ORDER BY checkin_date ASC")
    List<LocalDate> listCheckinDates(@Param("planId") Long planId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM plan_checkin WHERE plan_id = #{planId} AND user_id = #{userId}")
    int countByPlanAndUser(@Param("planId") Long planId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM plan_checkin WHERE plan_id = #{planId} AND user_id = #{userId} AND checkin_date = #{date}")
    int countByDate(@Param("planId") Long planId, @Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT MAX(checkin_date) FROM plan_checkin WHERE plan_id = #{planId} AND user_id = #{userId}")
    LocalDate lastCheckinDate(@Param("planId") Long planId, @Param("userId") Long userId);

    @Insert("INSERT INTO plan_checkin (plan_id, user_id, checkin_date, remark) VALUES (#{planId}, #{userId}, #{checkinDate}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlanCheckin checkin);
}
