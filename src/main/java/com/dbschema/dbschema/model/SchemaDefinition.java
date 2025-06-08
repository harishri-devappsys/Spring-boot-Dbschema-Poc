package com.dbschema.dbschema.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "schema_definitions")
public class SchemaDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "table_name", unique = true, nullable = false)
    private String tableName;
    
    @Column(name = "schema_json", columnDefinition = "TEXT")
    private String schemaJson;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "is_active")
    private Boolean isActive = true;
}