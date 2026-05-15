package com.example.infoorg.mapper;

import com.example.infoorg.entity.Entry;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EntryMapper {

    @Insert("""
            INSERT INTO entries (
              id, user_id, topic_id, raw_content, content_type, source_type, source_title, source_link,
              captured_at, insight_text, deleted, version,
              image_path, image_ocr_text, url, url_title, url_description, url_extracted_text
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
              #{insightText},
              #{deleted},
              COALESCE(#{version}, 0),
              #{imagePath},
              #{imageOcrText},
              #{url},
              #{urlTitle},
              #{urlDescription},
              #{urlExtractedText}
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
              e.version,
              e.created_at,
              e.updated_at,
              e.image_path,
              e.image_ocr_text,
              e.url,
              e.url_title,
              e.url_description,
              e.url_extracted_text
            FROM entries e
            LEFT JOIN topics t ON e.topic_id = t.id AND t.deleted = 0
            WHERE e.deleted = 0
            ORDER BY e.captured_at DESC
            LIMIT 5
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
            @Result(column = "version", property = "version"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt"),
            @Result(column = "image_path", property = "imagePath"),
            @Result(column = "image_ocr_text", property = "imageOcrText"),
            @Result(column = "url", property = "url"),
            @Result(column = "url_title", property = "urlTitle"),
            @Result(column = "url_description", property = "urlDescription"),
            @Result(column = "url_extracted_text", property = "urlExtractedText")
    })
    List<Entry> selectRecentEntries();

    @Update("""
            UPDATE entries
            SET insight_text = #{insightText}, version = version + 1
            WHERE id = CAST(#{entryId} AS uuid) AND deleted = 0 AND version = #{version}
            """)
    int updateInsightText(@Param("entryId") String entryId,
                          @Param("insightText") String insightText,
                          @Param("version") int version);

    @Update("""
            UPDATE entries
            SET topic_id = CAST(#{topicId} AS uuid), version = version + 1
            WHERE id = CAST(#{entryId} AS uuid) AND deleted = 0 AND version = #{version}
            """)
    int updateTopicId(@Param("entryId") String entryId,
                      @Param("topicId") String topicId,
                      @Param("version") int version);

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
              e.version,
              e.created_at,
              e.updated_at,
              e.image_path,
              e.image_ocr_text,
              e.url,
              e.url_title,
              e.url_description,
              e.url_extracted_text
            FROM entries e
            LEFT JOIN topics t ON e.topic_id = t.id AND t.deleted = 0
            WHERE e.deleted = 0
              AND e.user_id = CAST(#{userId} AS uuid)
              AND (e.insight_text IS NULL OR e.topic_id IS NULL)
              AND (e.skipped_at IS NULL OR e.skipped_at < NOW() - INTERVAL '24 hours')
            ORDER BY
              (e.insight_text IS NULL) DESC,
              e.captured_at DESC,
              (e.skipped_at IS NULL) DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    @ResultMap("entryResult")
    List<Entry> selectPendingEntries(@Param("userId") String userId,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

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
              e.version,
              e.created_at,
              e.updated_at,
              e.image_path,
              e.image_ocr_text,
              e.url,
              e.url_title,
              e.url_description,
              e.url_extracted_text
            FROM entries e
            LEFT JOIN topics t ON e.topic_id = t.id AND t.deleted = 0
            WHERE e.deleted = 0
              AND e.user_id = CAST(#{userId} AS uuid)
              AND (e.insight_text IS NULL OR e.topic_id IS NULL)
              AND (e.skipped_at IS NULL OR e.skipped_at < NOW() - INTERVAL '24 hours')
              AND e.captured_at < CAST(#{cursor} AS timestamp)
            ORDER BY e.captured_at DESC
            LIMIT #{limit}
            """)
    @ResultMap("entryResult")
    List<Entry> selectPendingEntriesByCursor(@Param("userId") String userId,
                                              @Param("cursor") String cursor,
                                              @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM entries
            WHERE deleted = 0
              AND user_id = CAST(#{userId} AS uuid)
              AND (insight_text IS NULL OR topic_id IS NULL)
              AND (skipped_at IS NULL OR skipped_at < NOW() - INTERVAL '24 hours')
            """)
    long countPendingEntries(@Param("userId") String userId);

    @Update("""
            UPDATE entries
            SET skipped_at = NOW(), skip_count = skip_count + 1
            WHERE id = CAST(#{entryId} AS uuid) AND deleted = 0
            """)
    int skipEntry(@Param("entryId") String entryId);

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
              e.version,
              e.created_at,
              e.updated_at,
              e.image_path,
              e.image_ocr_text,
              e.url,
              e.url_title,
              e.url_description,
              e.url_extracted_text
            FROM entries e
            LEFT JOIN topics t ON e.topic_id = t.id AND t.deleted = 0
            WHERE e.id = CAST(#{entryId} AS uuid) AND e.deleted = 0
            """)
    @ResultMap("entryResult")
    Entry selectById(@Param("entryId") String entryId);

    @Select("""
            <script>
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
              e.version,
              e.created_at,
              e.updated_at,
              e.image_path,
              e.image_ocr_text,
              e.url,
              e.url_title,
              e.url_description,
              e.url_extracted_text
            FROM entries e
            LEFT JOIN topics t ON e.topic_id = t.id AND t.deleted = 0
            WHERE e.deleted = 0
              AND e.user_id = CAST(#{userId} AS uuid)
              AND (
                  e.search_vector @@ plainto_tsquery('jiebacfg', #{keyword})
                  OR e.raw_content ILIKE '%' || #{keyword} || '%'
                  OR e.insight_text ILIKE '%' || #{keyword} || '%'
                  OR e.url_title ILIKE '%' || #{keyword} || '%'
                  OR e.image_ocr_text ILIKE '%' || #{keyword} || '%'
              )
              <if test="topicId != null"> AND e.topic_id = CAST(#{topicId} AS uuid)</if>
              <if test="hasInsight != null and hasInsight"> AND e.insight_text IS NOT NULL</if>
              <if test="hasInsight != null and !hasInsight"> AND e.insight_text IS NULL</if>
              <if test="startDate != null"> AND e.captured_at &gt;= CAST(#{startDate} AS timestamp)</if>
              <if test="endDate != null"> AND e.captured_at &lt;= CAST(#{endDate} AS timestamp)</if>
              <if test="cursor != null"> AND e.captured_at &lt; CAST(#{cursor} AS timestamp)</if>
            ORDER BY e.captured_at DESC
            LIMIT #{limit}
            </script>
            """)
    @ResultMap("entryResult")
    List<Entry> searchEntries(@Param("userId") String userId,
                              @Param("keyword") String keyword,
                              @Param("topicId") String topicId,
                              @Param("hasInsight") Boolean hasInsight,
                              @Param("startDate") String startDate,
                              @Param("endDate") String endDate,
                              @Param("cursor") String cursor,
                              @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM entries e
            WHERE e.deleted = 0
              AND e.user_id = CAST(#{userId} AS uuid)
              AND (
                  e.search_vector @@ plainto_tsquery('jiebacfg', #{keyword})
                  OR e.raw_content ILIKE '%' || #{keyword} || '%'
                  OR e.insight_text ILIKE '%' || #{keyword} || '%'
                  OR e.url_title ILIKE '%' || #{keyword} || '%'
                  OR e.image_ocr_text ILIKE '%' || #{keyword} || '%'
              )
              <if test="topicId != null"> AND e.topic_id = CAST(#{topicId} AS uuid)</if>
              <if test="hasInsight != null and hasInsight"> AND e.insight_text IS NOT NULL</if>
              <if test="hasInsight != null and !hasInsight"> AND e.insight_text IS NULL</if>
              <if test="startDate != null"> AND e.captured_at &gt;= CAST(#{startDate} AS timestamp)</if>
              <if test="endDate != null"> AND e.captured_at &lt;= CAST(#{endDate} AS timestamp)</if>
            </script>
            """)
    long countSearchEntries(@Param("userId") String userId,
                            @Param("keyword") String keyword,
                            @Param("topicId") String topicId,
                            @Param("hasInsight") Boolean hasInsight,
                            @Param("startDate") String startDate,
                            @Param("endDate") String endDate);

    @Select("""
            <script>
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
              e.version,
              e.created_at,
              e.updated_at,
              e.image_path,
              e.image_ocr_text,
              e.url,
              e.url_title,
              e.url_description,
              e.url_extracted_text
            FROM entries e
            LEFT JOIN topics t ON e.topic_id = t.id AND t.deleted = 0
            WHERE e.deleted = 0
              AND e.topic_id = CAST(#{topicId} AS uuid)
              <if test="cursor != null"> AND e.captured_at &lt; CAST(#{cursor} AS timestamp)</if>
            ORDER BY e.captured_at DESC
            LIMIT #{limit}
            </script>
            """)
    @ResultMap("entryResult")
    List<Entry> selectByTopicId(@Param("topicId") String topicId,
                                @Param("cursor") String cursor,
                                @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM entries
            WHERE deleted = 0 AND topic_id = CAST(#{topicId} AS uuid)
            """)
    long countByTopicId(@Param("topicId") String topicId);

    @Update("""
            UPDATE entries SET deleted = 1
            WHERE id = CAST(#{entryId} AS uuid) AND deleted = 0
            """)
    int softDelete(@Param("entryId") String entryId);

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
              e.version,
              e.created_at,
              e.updated_at,
              e.image_path,
              e.image_ocr_text,
              e.url,
              e.url_title,
              e.url_description,
              e.url_extracted_text
            FROM entries e
            LEFT JOIN topics t ON e.topic_id = t.id AND t.deleted = 0
            INNER JOIN (
              SELECT entry_id, COUNT(*) AS cnt
              FROM reuse_records
              GROUP BY entry_id
              HAVING COUNT(*) >= 2
            ) r ON e.id = r.entry_id
            WHERE e.deleted = 0
              AND e.user_id = CAST(#{userId} AS uuid)
            ORDER BY r.cnt DESC, e.captured_at DESC
            LIMIT #{limit}
            """)
    @ResultMap("entryResult")
    List<Entry> selectReviewEntries(@Param("userId") String userId,
                                    @Param("limit") int limit);

    @Select("""
            <script>
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
              e.version,
              e.created_at,
              e.updated_at,
              e.image_path,
              e.image_ocr_text,
              e.url,
              e.url_title,
              e.url_description,
              e.url_extracted_text
            FROM entries e
            LEFT JOIN topics t ON e.topic_id = t.id AND t.deleted = 0
            WHERE e.deleted = 0
              AND e.user_id = CAST(#{userId} AS uuid)
              <if test="contentType != null and contentType != ''">AND e.content_type = #{contentType}</if>
              <if test="topicId != null and topicId != ''">AND e.topic_id = CAST(#{topicId} AS uuid)</if>
            ORDER BY e.captured_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    @ResultMap("entryResult")
    List<Entry> selectEntriesPage(@Param("userId") String userId,
                                  @Param("contentType") String contentType,
                                  @Param("topicId") String topicId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM entries e
            WHERE e.deleted = 0
              AND e.user_id = CAST(#{userId} AS uuid)
              <if test="contentType != null and contentType != ''">AND e.content_type = #{contentType}</if>
              <if test="topicId != null and topicId != ''">AND e.topic_id = CAST(#{topicId} AS uuid)</if>
            </script>
            """)
    long countEntriesPage(@Param("userId") String userId,
                          @Param("contentType") String contentType,
                          @Param("topicId") String topicId);

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
              e.version,
              e.created_at,
              e.updated_at,
              e.image_path,
              e.image_ocr_text,
              e.url,
              e.url_title,
              e.url_description,
              e.url_extracted_text
            FROM entries e
            LEFT JOIN topics t ON e.topic_id = t.id AND t.deleted = 0
            WHERE e.deleted = 0
              AND e.user_id = CAST(#{userId} AS uuid)
            ORDER BY e.captured_at DESC
            LIMIT #{limit}
            """)
    @ResultMap("entryResult")
    List<Entry> selectEntriesRecentForUser(@Param("userId") String userId, @Param("limit") int limit);

    @Select("""
            <script>
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
              e.version,
              e.created_at,
              e.updated_at,
              e.image_path,
              e.image_ocr_text,
              e.url,
              e.url_title,
              e.url_description,
              e.url_extracted_text
            FROM entries e
            LEFT JOIN topics t ON e.topic_id = t.id AND t.deleted = 0
            WHERE e.deleted = 0
              AND e.user_id = CAST(#{userId} AS uuid)
              <if test="topicId != null and topicId != ''">AND e.topic_id = CAST(#{topicId} AS uuid)</if>
              <if test="contentType != null and contentType != ''">AND e.content_type = #{contentType}</if>
              <if test="hasInsight != null and hasInsight">AND e.insight_text IS NOT NULL</if>
              <if test="hasInsight != null and !hasInsight">AND e.insight_text IS NULL</if>
              <if test="startDate != null and startDate != ''">AND e.captured_at &gt;= CAST(#{startDate} AS timestamp)</if>
              <if test="endDate != null and endDate != ''">AND e.captured_at &lt;= CAST(#{endDate} AS timestamp)</if>
              <if test="cursor != null and cursor != ''">AND e.captured_at &lt; CAST(#{cursor} AS timestamp)</if>
            ORDER BY e.captured_at DESC
            LIMIT #{limit}
            </script>
            """)
    @ResultMap("entryResult")
    List<Entry> browseEntries(@Param("userId") String userId,
                              @Param("topicId") String topicId,
                              @Param("contentType") String contentType,
                              @Param("hasInsight") Boolean hasInsight,
                              @Param("startDate") String startDate,
                              @Param("endDate") String endDate,
                              @Param("cursor") String cursor,
                              @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM entries e
            WHERE e.deleted = 0
              AND e.user_id = CAST(#{userId} AS uuid)
              <if test="topicId != null and topicId != ''">AND e.topic_id = CAST(#{topicId} AS uuid)</if>
              <if test="contentType != null and contentType != ''">AND e.content_type = #{contentType}</if>
              <if test="hasInsight != null and hasInsight">AND e.insight_text IS NOT NULL</if>
              <if test="hasInsight != null and !hasInsight">AND e.insight_text IS NULL</if>
              <if test="startDate != null and startDate != ''">AND e.captured_at &gt;= CAST(#{startDate} AS timestamp)</if>
              <if test="endDate != null and endDate != ''">AND e.captured_at &lt;= CAST(#{endDate} AS timestamp)</if>
            </script>
            """)
    long countBrowseEntries(@Param("userId") String userId,
                            @Param("topicId") String topicId,
                            @Param("contentType") String contentType,
                            @Param("hasInsight") Boolean hasInsight,
                            @Param("startDate") String startDate,
                            @Param("endDate") String endDate);

    @Update("""
            <script>
            UPDATE entries
            <set>
              <if test="rawContent != null">raw_content = #{rawContent},</if>
              <if test="insightText != null">insight_text = #{insightText},</if>
              <if test="topicId != null">topic_id = CAST(#{topicId} AS uuid),</if>
              <if test="sourceType != null">source_type = #{sourceType},</if>
              <if test="sourceTitle != null">source_title = #{sourceTitle},</if>
              <if test="sourceLink != null">source_link = #{sourceLink},</if>
              <if test="contentType != null">content_type = #{contentType},</if>
              <if test="imagePath != null">image_path = #{imagePath},</if>
              <if test="imageOcrText != null">image_ocr_text = #{imageOcrText},</if>
              <if test="url != null">url = #{url},</if>
              <if test="urlTitle != null">url_title = #{urlTitle},</if>
              <if test="urlDescription != null">url_description = #{urlDescription},</if>
              <if test="urlExtractedText != null">url_extracted_text = #{urlExtractedText},</if>
              version = version + 1,
              updated_at = NOW()
            </set>
            WHERE id = CAST(#{id} AS uuid) AND deleted = 0 AND version = #{version}
            </script>
            """)
    int updateEntrySelective(Entry entry);
}
