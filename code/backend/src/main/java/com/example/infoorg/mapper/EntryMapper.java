package com.example.infoorg.mapper;

import com.example.infoorg.entity.Entry;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EntryMapper {

    @Insert("""
            INSERT INTO entries (
              id, user_id, topic_id, raw_content, content_type, source_type, source_title, source_link, captured_at, deleted
            ) VALUES (
              CAST(#{id} AS uuid),
              CAST(#{userId} AS uuid),
              CAST(#{topicId} AS uuid),
              #{rawContent},
              #{contentType},
              #{sourceType},
              #{sourceTitle},
              #{sourceLink},
              #{capturedAt},
              #{deleted}
            )
            """)
    int insertEntry(Entry entry);

    @Select("""
            SELECT
              e.id::text AS id,
              e.user_id::text AS user_id,
              e.raw_content,
              e.content_type,
              e.source_type,
              e.source_title,
              e.source_link,
              e.captured_at,
              e.insight_text,
              e.topic_id::text AS topic_id,
              t.name AS topic_name,
              e.deleted,
              e.created_at,
              e.updated_at
            FROM entries e
            LEFT JOIN topics t ON e.topic_id = t.id AND t.deleted = 0
            WHERE e.deleted = 0
            ORDER BY e.captured_at DESC
            LIMIT 10
            """)
    @Results(id = "entryResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "raw_content", property = "rawContent"),
            @Result(column = "content_type", property = "contentType"),
            @Result(column = "source_type", property = "sourceType"),
            @Result(column = "source_title", property = "sourceTitle"),
            @Result(column = "source_link", property = "sourceLink"),
            @Result(column = "captured_at", property = "capturedAt"),
            @Result(column = "insight_text", property = "insightText"),
            @Result(column = "topic_id", property = "topicId"),
            @Result(column = "topic_name", property = "topicName"),
            @Result(column = "deleted", property = "deleted"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<Entry> selectRecentEntries();
}
