package com.healthapp.mapper;

import com.healthapp.entity.UserSettings;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;

@Mapper
public interface UserSettingsMapper {
    @Select("SELECT * FROM user_settings WHERE user_id = #{userId}")
    UserSettings findByUserId(Long userId);

    @Insert("INSERT IGNORE INTO user_settings (user_id, notify_enabled, step_goal, privacy_level) VALUES (#{userId}, 1, 8000, 'partial')")
    int ensureDefault(Long userId);

    @UpdateProvider(type = SqlProvider.class, method = "updatePartial")
    int updatePartial(@Param("userId") Long userId, @Param("notifyEnabled") Integer notifyEnabled, @Param("stepGoal") Integer stepGoal, @Param("privacyLevel") String privacyLevel);

    class SqlProvider {
        public String updatePartial(java.util.Map<String, Object> params) {
            return new SQL() {{
                UPDATE("user_settings");
                if (params.get("notifyEnabled") != null) SET("notify_enabled = #{notifyEnabled}");
                if (params.get("stepGoal") != null) SET("step_goal = #{stepGoal}");
                if (params.get("privacyLevel") != null) SET("privacy_level = #{privacyLevel}");
                WHERE("user_id = #{userId}");
            }}.toString();
        }
    }
}
