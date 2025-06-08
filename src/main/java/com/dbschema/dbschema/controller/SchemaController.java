package com.dbschema.dbschema.controller;

import com.dbschema.dbschema.dto.TableSchema;
import com.dbschema.dbschema.model.SchemaDefinition;
import com.dbschema.dbschema.model.SchemaChangeLog;
import com.dbschema.dbschema.service.SchemaService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SchemaController {
    
    private final SchemaService schemaService;
    
    @PostMapping("/create")
    public ResponseEntity<String> createSchema(@RequestBody TableSchema schema) {
        try {
            schemaService.createOrUpdateSchema(schema);
            return ResponseEntity.ok("Schema processed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to process schema: " + e.getMessage());
        }
    }
    
    @PostMapping("/update")
    public ResponseEntity<String> updateSchema(@RequestBody UpdateSchemaRequest request) {
        try {
            schemaService.createOrUpdateSchema(request.getNewSchema());
            return ResponseEntity.ok("Schema updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update schema: " + e.getMessage());
        }
    }
    
    @GetMapping("/changes")
    public ResponseEntity<List<SchemaChangeLog>> getSchemaChanges(
            @RequestParam(required = false) String tableName) {
        return ResponseEntity.ok(schemaService.getSchemaChanges(tableName));
    }
    
    @GetMapping("/current/{tableName}")
    public ResponseEntity<TableSchema> getCurrentSchema(@PathVariable String tableName) {
        Optional<TableSchema> schema = schemaService.getCurrentSchema(tableName);
        return schema.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}

// DTOs
@Data
class UpdateSchemaRequest {
    private TableSchema oldSchema;
    private TableSchema newSchema;
}