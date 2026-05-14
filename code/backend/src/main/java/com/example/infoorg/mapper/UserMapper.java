package com.example.infoorg.mapper;

import org.apache.ibatis.annotations.*;

import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id::text AS id, username, password_hash, email
            FROM users
            WHERE username = #{username} AND deleted = 0
            """)
    Map<String, Object> findByUsername(@Param("username") String username);

    @Select("""
            SELECT id::text AS id, username, email
            FROM users
            WHERE id = CAST(#{userId} AS uuid) AND deleted = 0
            """)
    Map<String, Object> findById(@Param("userId") String userId);
}
