package com.healthapp.mapper;

import com.healthapp.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM `user` WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM `user` WHERE username = #{account} OR phone = #{account} LIMIT 1")
    User findByUsernameOrPhone(String account);

    @Select("SELECT * FROM `user` ORDER BY created_at DESC, id DESC")
    List<User> selectAll();

    @Select("SELECT COUNT(*) FROM `user`")
    long countAll();

    @Select("SELECT COUNT(*) FROM `user` WHERE username = #{username}")
    int countByUsername(String username);

    @Select("SELECT COUNT(*) FROM `user` WHERE phone = #{phone} AND id <> #{excludeId}")
    int countByPhone(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    @Insert("INSERT INTO `user` (username, phone, password, nickname, gender, birthday, avatar, role, status) " +
        "VALUES (#{username}, #{phone}, #{password}, #{nickname}, #{gender}, #{birthday}, #{avatar}, #{role}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE `user` SET nickname=#{nickname}, phone=#{phone}, gender=#{gender}, birthday=#{birthday}, avatar=#{avatar} WHERE id=#{id}")
    int updateProfile(User user);

    @Update("UPDATE `user` SET password=#{password} WHERE id=#{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
