package com.dbschema.dbschema.util;

import com.dbschema.dbschema.dto.TableSchema;
import com.dbschema.dbschema.dto.ColumnDefinition;
import com.dbschema.dbschema.exception.SchemaException;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class SchemaValidator {
    
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");
    private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");
    private static final Set<String> RESERVED_KEYWORDS = Set.of(
        "SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER", "TABLE",
        "INDEX", "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "CONSTRAINT", "UNIQUE",
        "NOT", "NULL", "DEFAULT", "AUTO_INCREMENT", "SERIAL"
    );
    
    public void validateSchema(TableSchema schema) {
        if (schema == null) {
            throw new SchemaException("Schema cannot be null");
        }
        
        validateTableName(schema.getTableName());
        validateColumns(schema.getColumns());
    }
    
    private void validateTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new SchemaException("Table name cannot be null or empty");
        }
        
        if (!TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new SchemaException("Invalid table name format: " + tableName);
        }
        
        if (RESERVED_KEYWORDS.contains(tableName.toUpperCase())) {
            throw new SchemaException("Table name cannot be a reserved keyword: " + tableName);
        }
        
        if (tableName.length() > 63) {
            throw new SchemaException("Table name too long (max 63 characters): " + tableName);
        }
    }
    
    private void validateColumns(java.util.List<ColumnDefinition> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new SchemaException("Table must have at least one column");
        }
        
        Set<String> columnNames = new HashSet<>();
        boolean hasPrimaryKey = false;
        
        for (ColumnDefinition column : columns) {
            validateColumnName(column.getName());
            validateColumnType(column.getType());
            
            if (columnNames.contains(column.getName().toLowerCase())) {
                throw new SchemaException("Duplicate column name: " + column.getName());
            }
            columnNames.add(column.getName().toLowerCase());
            
            if (column.getPrimaryKey() != null && column.getPrimaryKey()) {
                if (hasPrimaryKey) {
                    throw new SchemaException("Table can have only one primary key");
                }
                hasPrimaryKey = true;
            }
        }
    }
    
    private void validateColumnName(String columnName) {
        if (columnName == null || columnName.trim().isEmpty()) {
            throw new SchemaException("Column name cannot be null or empty");
        }
        
        if (!COLUMN_NAME_PATTERN.matcher(columnName).matches()) {
            throw new SchemaException("Invalid column name format: " + columnName);
        }
        
        if (RESERVED_KEYWORDS.contains(columnName.toUpperCase())) {
            throw new SchemaException("Column name cannot be a reserved keyword: " + columnName);
        }
    }
    
    private void validateColumnType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new SchemaException("Column type cannot be null or empty");
        }
        
        String normalizedType = type.toLowerCase();
        if (!isValidDataType(normalizedType)) {
            throw new SchemaException("Invalid column type: " + type);
        }
    }
    
    private boolean isValidDataType(String type) {
        return type.matches("^(varchar|text|int|integer|boolean|bool|date|timestamp|decimal|bigint|smallint|char).*");
    }
}