package com.dbschema.dbschema.service;

import com.dbschema.dbschema.dto.TableSchema;
import com.dbschema.dbschema.dto.ColumnDefinition;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SQLGenerator {
    
    public String generateCreateTableSQL(TableSchema schema) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(schema.getTableName()).append(" (\n");
        
        List<String> columnDefs = new ArrayList<>();
        List<String> indexes = new ArrayList<>();
        
        for (ColumnDefinition col : schema.getColumns()) {
            StringBuilder colDef = new StringBuilder();
            colDef.append("    ").append(col.getName()).append(" ");
            colDef.append(mapDataType(col.getType()));
            
            if (col.getPrimaryKey() != null && col.getPrimaryKey()) {
                colDef.append(" PRIMARY KEY");
            }
            
            if (col.getNullable() != null && !col.getNullable()) {
                colDef.append(" NOT NULL");
            }
            
            if (col.getUnique() != null && col.getUnique()) {
                colDef.append(" UNIQUE");
            }
            
            if (col.getDefaultValue() != null) {
                colDef.append(" DEFAULT ").append(col.getDefaultValue());
            }
            
            columnDefs.add(colDef.toString());
            
            // Collect index information
            if (col.getIndex() != null && col.getIndex()) {
                indexes.add("CREATE INDEX IF NOT EXISTS idx_" + schema.getTableName() + "_" + col.getName() + 
                           " ON " + schema.getTableName() + " (" + col.getName() + ");");
            }
        }
        
        sql.append(String.join(",\n", columnDefs));
        sql.append("\n);");
        
        // Add index creation statements
        if (!indexes.isEmpty()) {
            sql.append("\n\n");
            sql.append(String.join("\n", indexes));
        }
        
        return sql.toString();
    }
    
    //this function needs to be optimized
    public List<String> generateAlterTableSQL(TableSchema oldSchema, TableSchema newSchema) {
        List<String> alterStatements = new ArrayList<>();
        String tableName = newSchema.getTableName();
        
        // Get column maps for comparison
        Map<String, ColumnDefinition> oldColumns = oldSchema.getColumns().stream()
            .collect(Collectors.toMap(ColumnDefinition::getName, col -> col));
        Map<String, ColumnDefinition> newColumns = newSchema.getColumns().stream()
            .collect(Collectors.toMap(ColumnDefinition::getName, col -> col));
        
        // Handle primary key changes first (before other column modifications)
        handlePrimaryKeyChanges(oldColumns, newColumns, tableName, alterStatements);
        
        // Find columns to add
        for (String colName : newColumns.keySet()) {
            if (!oldColumns.containsKey(colName)) {
                ColumnDefinition newCol = newColumns.get(colName);
                alterStatements.add(generateAddColumnSQL(tableName, newCol));
            }
        }
        
        // Find columns to drop
        for (String colName : oldColumns.keySet()) {
            if (!newColumns.containsKey(colName)) {
                alterStatements.add("ALTER TABLE " + tableName + " DROP COLUMN IF EXISTS " + colName + ";");
            }
        }
        
        // Find columns to modify (excluding primary key changes which are handled separately)
        for (String colName : newColumns.keySet()) {
            if (oldColumns.containsKey(colName)) {
                ColumnDefinition oldCol = oldColumns.get(colName);
                ColumnDefinition newCol = newColumns.get(colName);
                
                if (!areColumnsEqual(oldCol, newCol)) {
                    // Handle non-primary key changes
                    if (!Objects.equals(oldCol.getType(), newCol.getType()) ||
                        !Objects.equals(oldCol.getNullable(), newCol.getNullable()) ||
                        !Objects.equals(oldCol.getDefaultValue(), newCol.getDefaultValue())) {
                        alterStatements.add(generateModifyColumnSQL(tableName, newCol));
                    }
                    
                    // Handle unique constraint changes
                    if (!Objects.equals(oldCol.getUnique(), newCol.getUnique())) {
                        if (newCol.getUnique() != null && newCol.getUnique()) {
                            alterStatements.add("ALTER TABLE " + tableName + " ADD CONSTRAINT uk_" + tableName + "_" + colName + " UNIQUE (" + colName + ");");
                        } else {
                            alterStatements.add("ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS uk_" + tableName + "_" + colName + ";");
                        }
                    }
                }
                
                // Handle index changes
                if (!Objects.equals(oldCol.getIndex(), newCol.getIndex())) {
                    if (newCol.getIndex() != null && newCol.getIndex()) {
                        alterStatements.add("CREATE INDEX IF NOT EXISTS idx_" + tableName + "_" + colName + 
                                          " ON " + tableName + " (" + colName + ");");
                    } else {
                        alterStatements.add("DROP INDEX IF EXISTS idx_" + tableName + "_" + colName + ";");
                    }
                }
            }
        }
        
        return alterStatements;
    }
    
    private void handlePrimaryKeyChanges(Map<String, ColumnDefinition> oldColumns, 
                                       Map<String, ColumnDefinition> newColumns, 
                                       String tableName, 
                                       List<String> alterStatements) {
        
        // Find old primary key columns
        Set<String> oldPrimaryKeys = oldColumns.entrySet().stream()
            .filter(entry -> entry.getValue().getPrimaryKey() != null && entry.getValue().getPrimaryKey())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        // Find new primary key columns
        Set<String> newPrimaryKeys = newColumns.entrySet().stream()
            .filter(entry -> entry.getValue().getPrimaryKey() != null && entry.getValue().getPrimaryKey())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        // Check if primary key configuration has changed
        if (!oldPrimaryKeys.equals(newPrimaryKeys)) {
            // Drop existing primary key constraint if it exists (PostgreSQL syntax)
            if (!oldPrimaryKeys.isEmpty()) {
                // PostgreSQL automatically names the primary key constraint as tablename_pkey
                alterStatements.add("ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + tableName + "_pkey;");
            }
            
            // Add new primary key constraint if specified
            if (!newPrimaryKeys.isEmpty()) {
                String primaryKeyColumns = String.join(", ", newPrimaryKeys);
                alterStatements.add("ALTER TABLE " + tableName + " ADD CONSTRAINT " + tableName + "_pkey PRIMARY KEY (" + primaryKeyColumns + ");");
            }
        }
    }
    
    private String generateAddColumnSQL(String tableName, ColumnDefinition col) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(tableName).append(" ADD COLUMN ");
        sql.append(col.getName()).append(" ").append(mapDataType(col.getType()));
        
        if (col.getNullable() != null && !col.getNullable()) {
            sql.append(" NOT NULL");
        }
        
        if (col.getDefaultValue() != null) {
            sql.append(" DEFAULT ").append(col.getDefaultValue());
        }
        
        // Note: Primary key constraint for new columns is handled in handlePrimaryKeyChanges
        
        sql.append(";");
        return sql.toString();
    }
    
    private String generateModifyColumnSQL(String tableName, ColumnDefinition col) {
        // PostgreSQL requires separate ALTER statements for different column modifications
        List<String> alterParts = new ArrayList<>();
        
        // Type change
        alterParts.add("ALTER TABLE " + tableName + " ALTER COLUMN " + col.getName() + " TYPE " + mapDataType(col.getType()));
        
        // Handle nullability changes
        if (col.getNullable() != null) {
            if (col.getNullable()) {
                alterParts.add("ALTER TABLE " + tableName + " ALTER COLUMN " + col.getName() + " DROP NOT NULL");
            } else {
                alterParts.add("ALTER TABLE " + tableName + " ALTER COLUMN " + col.getName() + " SET NOT NULL");
            }
        }
        
        // Handle default value changes  
        if (col.getDefaultValue() != null) {
            alterParts.add("ALTER TABLE " + tableName + " ALTER COLUMN " + col.getName() + " SET DEFAULT " + col.getDefaultValue());
        } else {
            // If defaultValue is explicitly null, drop the default
            alterParts.add("ALTER TABLE " + tableName + " ALTER COLUMN " + col.getName() + " DROP DEFAULT");
        }
        
        return String.join(";\n", alterParts) + ";";
    }
    
    private boolean areColumnsEqual(ColumnDefinition old, ColumnDefinition newCol) {
        return Objects.equals(old.getType(), newCol.getType()) &&
               Objects.equals(old.getNullable(), newCol.getNullable()) &&
               Objects.equals(old.getDefaultValue(), newCol.getDefaultValue()) &&
               Objects.equals(old.getUnique(), newCol.getUnique()) &&
               Objects.equals(old.getPrimaryKey(), newCol.getPrimaryKey()) && // Added primary key check
               Objects.equals(old.getIndex(), newCol.getIndex());
    }
    
    private String mapDataType(String type) {
        if (type.toLowerCase().startsWith("varchar")) {
            return type.toUpperCase();
        }
        
        switch (type.toLowerCase()) {
            case "int":
            case "integer":
                return "INTEGER";
            case "string":
                return "VARCHAR(255)";
            case "text":
                return "TEXT";
            case "boolean":
            case "bool":
                return "BOOLEAN";
            case "date":
                return "DATE";
            case "timestamp":
                return "TIMESTAMP";
            case "decimal":
                return "DECIMAL";
            default:
                return type.toUpperCase();
        }
    }
}