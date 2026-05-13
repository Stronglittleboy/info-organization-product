package com.example.infoorg.mapper;

import com.example.infoorg.entity.Topic;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TopicMapper {

    @Select("""
            SELECT id::text AS id,
                   user_id::text AS user_id,
                   name,
                   description,
                   created_at,
                   updated_at,
                   deleted
            FROM topics
            WHERE user_id = CAST(#{userId} AS uuid)
              AND name = #{name}
              AND deleted = 0
            LIMIT 1
            """)
    @Results(id = "topicResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "name", property = "name"),
            @Result(column = "description", property = "description"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(column = "deleted", property = "deleted")
    })
    Topic findByUserIdAndName(String userId, String name);

    @Insert("""
            INSERT INTO topics (id, user_id, name, deleted)
            VALUES (CAST(#{id} AS uuid), CAST(#{userId} AS uuid), #{name}, #{deleted})
            """)
    int insertTopic(Topic topic);
}
