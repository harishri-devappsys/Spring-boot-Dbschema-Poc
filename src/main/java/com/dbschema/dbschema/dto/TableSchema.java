package com.dbschema.dbschema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class TableSchema {
    @JsonProperty("tablename")
    private String tableName;
    
    @JsonProperty("columns")
    private List<ColumnDefinition> columns;
}