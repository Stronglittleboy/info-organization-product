package com.example.infoorg.mapper;

import com.example.infoorg.entity.Topic;
import org.apache.ibatis.annotations.*;

import java.util.List;

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

    @Select("""
            SELECT t.id::text AS id,
                   t.user_id::text AS user_id,
                   t.name,
                   t.description,
                   t.created_at,
                   t.updated_at,
                   t.deleted,
                   COUNT(e.id) AS entry_count
            FROM topics t
            LEFT JOIN entries e ON e.topic_id = t.id AND e.deleted = 0
            WHERE t.user_id = CAST(#{userId} AS uuid)
              AND t.deleted = 0
            GROUP BY t.id
            ORDER BY t.updated_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    @Results(id = "topicWithCount", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "name", property = "name"),
            @Result(column = "description", property = "description"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(column = "deleted", property = "deleted"),
            @Result(column = "entry_count", property = "entryCount")
    })
    List<Topic> selectTopicList(@Param("userId") String userId,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    @Select("""
            SELECT t.id::text AS id,
                   t.user_id::text AS user_id,
                   t.name,
                   t.description,
                   t.created_at,
                   t.updated_at,
                   t.deleted,
                   COUNT(e.id) AS entry_count
            FROM topics t
            LEFT JOIN entries e ON e.topic_id = t.id AND e.deleted = 0
            WHERE t.user_id = CAST(#{userId} AS uuid)
              AND t.deleted = 0
              AND t.name ILIKE '%' || #{keyword} || '%'
            GROUP BY t.id
            ORDER BY t.updated_at DESC
            LIMIT #{limit}
            """)
    @ResultMap("topicWithCount")
    List<Topic> searchTopicsByName(@Param("userId") String userId,
                                   @Param("keyword") String keyword,
                                   @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM topics
            WHERE user_id = CAST(#{userId} AS uuid) AND deleted = 0
            """)
    long countTopics(@Param("userId") String userId);

    @Select("""
            SELECT t.id::text AS id,
                   t.user_id::text AS user_id,
                   t.name,
                   t.description,
                   t.created_at,
                   t.updated_at,
                   t.deleted,
                   COUNT(e.id) AS entry_count
            FROM topics t
            LEFT JOIN entries e ON e.topic_id = t.id AND e.deleted = 0
            WHERE t.id = CAST(#{topicId} AS uuid) AND t.deleted = 0
            GROUP BY t.id
            """)
    @ResultMap("topicWithCount")
    Topic selectById(@Param("topicId") String topicId);
}
