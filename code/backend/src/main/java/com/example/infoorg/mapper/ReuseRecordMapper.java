package com.example.infoorg.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface ReuseRecordMapper {

    @Insert("""
            INSERT INTO reuse_records (id, entry_id, user_id, reuse_type)
            VALUES (CAST(#{id} AS uuid), CAST(#{entryId} AS uuid), CAST(#{userId} AS uuid), #{reuseType})
            """)
    int insertReuseRecord(@Param("id") String id,
                           @Param("entryId") String entryId,
                           @Param("userId") String userId,
                           @Param("reuseType") String reuseType);

    @Select("""
            SELECT COUNT(*) FROM reuse_records
            WHERE entry_id = CAST(#{entryId} AS uuid)
            """)
    int countByEntryId(@Param("entryId") String entryId);

    @Select("""
            SELECT MAX(reused_at) FROM reuse_records
            WHERE entry_id = CAST(#{entryId} AS uuid)
            """)
    java.time.LocalDateTime lastReusedAt(@Param("entryId") String entryId);
}
