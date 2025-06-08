package com.dbschema.dbschema.repository;

import com.dbschema.dbschema.model.SchemaDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SchemaDefinitionRepository extends JpaRepository<SchemaDefinition, Long> {
    Optional<SchemaDefinition> findByTableNameAndIsActive(String tableName, Boolean isActive);
    
    @Query("SELECT s FROM SchemaDefinition s WHERE s.tableName = :tableName AND s.isActive = true")
    Optional<SchemaDefinition> findLatestByTableName(String tableName);
}