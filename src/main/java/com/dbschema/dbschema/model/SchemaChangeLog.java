package com.dbschema.dbschema.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "schema_change_logs")
public class SchemaChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "table_name")
    private String tableName;
    
    @Column(name = "change_type")
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;
    
    @Column(name = "old_schema", columnDefinition = "TEXT")
    private String oldSchema;
    
    @Column(name = "new_schema", columnDefinition = "TEXT")
    private String newSchema;
    
    @Column(name = "sql_executed", columnDefinition = "TEXT")
    private String sqlExecuted;
    
    @Column(name = "executed_at")
    private LocalDateTime executedAt = LocalDateTime.now();
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}

