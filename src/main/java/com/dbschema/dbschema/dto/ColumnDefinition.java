package com.dbschema.dbschema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ColumnDefinition {
    private String name;
    private String type;
    private Boolean index = false;
    private Boolean nullable = true;
    
    @JsonProperty("primaryKey")
    private Boolean primaryKey = false;
    
    @JsonProperty("defaultValue")
    private String defaultValue;
    
    private Boolean unique = false;
}