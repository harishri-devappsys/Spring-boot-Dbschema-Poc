package com.dbschema.dbschema.repository;

import com.dbschema.dbschema.model.SchemaChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SchemaChangeLogRepository extends JpaRepository<SchemaChangeLog, Long> {
    List<SchemaChangeLog> findByTableNameOrderByExecutedAtDesc(String tableName);
    
    @Query("SELECT s FROM SchemaChangeLog s ORDER BY s.executedAt DESC")
    List<SchemaChangeLog> findAllOrderByExecutedAtDesc();
}