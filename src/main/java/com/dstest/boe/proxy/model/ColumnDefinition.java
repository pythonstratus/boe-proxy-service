package com.dstest.boe.proxy.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ColumnDefinition {

    private String name;
    private String type;
    private String displayLabel;
}
