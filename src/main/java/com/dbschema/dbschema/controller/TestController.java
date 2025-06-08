package com.dbschema.dbschema.controller;

import com.dbschema.dbschema.dto.TableSchema;
import com.dbschema.dbschema.dto.ColumnDefinition;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @GetMapping("/sample-schema")
    public TableSchema getSampleSchema() {
        TableSchema schema = new TableSchema();
        schema.setTableName("sample_table");
        
        ColumnDefinition id = new ColumnDefinition();
        id.setName("id");
        id.setType("INTEGER");
        id.setPrimaryKey(true);
        id.setNullable(false);
        
        ColumnDefinition name = new ColumnDefinition();
        name.setName("name");
        name.setType("VARCHAR(100)");
        name.setNullable(false);
        name.setIndex(true);
        
        ColumnDefinition email = new ColumnDefinition();
        email.setName("email");
        email.setType("VARCHAR(255)");
        email.setUnique(true);
        email.setIndex(true);
        
        ColumnDefinition createdAt = new ColumnDefinition();
        createdAt.setName("created_at");
        createdAt.setType("TIMESTAMP");
        createdAt.setDefaultValue("NOW()");
        createdAt.setNullable(false);
        
        schema.setColumns(Arrays.asList(id, name, email, createdAt));
        return schema;
    }
    
    @GetMapping("/health")
    public String health() {
        return "Schema Management POC is running!";
    }
}