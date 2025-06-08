package com.dbschema.dbschema.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dbschema.dbschema.dto.TableSchema;
import com.dbschema.dbschema.model.SchemaDefinition;
import com.dbschema.dbschema.model.SchemaChangeLog;
import com.dbschema.dbschema.model.ChangeType;
import com.dbschema.dbschema.model.ExecutionStatus;
import com.dbschema.dbschema.repository.SchemaDefinitionRepository;
import com.dbschema.dbschema.repository.SchemaChangeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaService {
    
    private final SchemaDefinitionRepository schemaRepository;
    private final SchemaChangeLogRepository changeLogRepository;
    private final SQLGenerator sqlGenerator;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final com.dbschema.dbschema.util.SchemaValidator schemaValidator;
    
    // Table-level locks for concurrent operations
    private final ConcurrentHashMap<String, ReentrantLock> tableLocks = new ConcurrentHashMap<>();
    
    @Transactional
    public void createOrUpdateSchema(TableSchema schema) {
        // Validate schema before processing
        schemaValidator.validateSchema(schema);
        
        String tableName = schema.getTableName();
        ReentrantLock lock = tableLocks.computeIfAbsent(tableName, k -> new ReentrantLock());
        
        lock.lock();
        try {
            Optional<SchemaDefinition> existingSchema = schemaRepository.findLatestByTableName(tableName);
            
            if (existingSchema.isPresent()) {
                updateSchema(existingSchema.get(), schema);
            } else {
                createNewSchema(schema);
            }
        } finally {
            lock.unlock();
        }
    }
    
    private void createNewSchema(TableSchema schema) {
        try {
            String sql = sqlGenerator.generateCreateTableSQL(schema);
            
            // Execute SQL
            jdbcTemplate.execute(sql);
            
            // Save schema definition
            SchemaDefinition definition = new SchemaDefinition();
            definition.setTableName(schema.getTableName());
            String schemaJson = objectMapper.writeValueAsString(schema);
            definition.setSchemaJson(schemaJson);
            definition.setVersion(1);
            schemaRepository.save(definition);
            
            // Log the change
            logSchemaChange(schema.getTableName(), ChangeType.CREATE_TABLE, null, 
                          schemaJson, sql, ExecutionStatus.SUCCESS, null);
            
            log.info("Successfully created table: {}", schema.getTableName());
            
        } catch (Exception e) {
            log.error("Failed to create table: {}", schema.getTableName(), e);
            logSchemaChange(schema.getTableName(), ChangeType.CREATE_TABLE, null, 
                          null, null, ExecutionStatus.FAILED, e.getMessage());
            throw new RuntimeException("Failed to create schema", e);
        }
    }
    
    private void updateSchema(SchemaDefinition existingDefinition, TableSchema newSchema) {
        try {
            String oldSchemaJson = existingDefinition.getSchemaJson();
            TableSchema oldSchema = objectMapper.readValue(oldSchemaJson, TableSchema.class);
            List<String> alterStatements = sqlGenerator.generateAlterTableSQL(oldSchema, newSchema);
            
            if (alterStatements.isEmpty()) {
                log.info("No changes detected for table: {}", newSchema.getTableName());
                return;
            }
            
            // Execute all alter statements
            StringBuilder executedSQL = new StringBuilder();
            for (String statement : alterStatements) {
                jdbcTemplate.execute(statement);
                executedSQL.append(statement).append("\n");
            }
            
            // Update schema definition
            String newSchemaJson = objectMapper.writeValueAsString(newSchema);
            existingDefinition.setSchemaJson(newSchemaJson);
            existingDefinition.setVersion(existingDefinition.getVersion() + 1);
            existingDefinition.setUpdatedAt(LocalDateTime.now());
            schemaRepository.save(existingDefinition);
            
            // Log the change
            logSchemaChange(newSchema.getTableName(), ChangeType.ALTER_TABLE, 
                          oldSchemaJson, newSchemaJson,
                          executedSQL.toString(), ExecutionStatus.SUCCESS, null);
            
            log.info("Successfully updated table: {} with {} changes", newSchema.getTableName(), alterStatements.size());
            
        } catch (Exception e) {
            log.error("Failed to update table: {}", newSchema.getTableName(), e);
            logSchemaChange(newSchema.getTableName(), ChangeType.ALTER_TABLE, 
                          existingDefinition.getSchemaJson(), null, null, ExecutionStatus.FAILED, e.getMessage());
            throw new RuntimeException("Failed to update schema", e);
        }
    }
    
    private void logSchemaChange(String tableName, ChangeType changeType, String oldSchema, 
                               String newSchema, String sqlExecuted, ExecutionStatus status, String errorMessage) {
        SchemaChangeLog changeLog = new SchemaChangeLog();
        changeLog.setTableName(tableName);
        changeLog.setChangeType(changeType);
        changeLog.setOldSchema(oldSchema);
        changeLog.setNewSchema(newSchema);
        changeLog.setSqlExecuted(sqlExecuted);
        changeLog.setStatus(status);
        changeLog.setErrorMessage(errorMessage);
        changeLog.setExecutedAt(LocalDateTime.now());
        
        changeLogRepository.save(changeLog);
    }
    
    public List<SchemaChangeLog> getSchemaChanges(String tableName) {
        if (tableName != null) {
            return changeLogRepository.findByTableNameOrderByExecutedAtDesc(tableName);
        }
        return changeLogRepository.findAllOrderByExecutedAtDesc();
    }
    
    public Optional<TableSchema> getCurrentSchema(String tableName) {
        Optional<SchemaDefinition> schemaDefinition = schemaRepository.findLatestByTableName(tableName);
        if (schemaDefinition.isPresent()) {
            try {
                return Optional.of(objectMapper.readValue(schemaDefinition.get().getSchemaJson(), TableSchema.class));
            } catch (Exception e) {
                log.error("Failed to deserialize schema for table: {}", tableName, e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}