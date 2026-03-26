package com.example.business.canal;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 表变更事件，封装 Canal 解析后的行数据
 */
@Data
@Builder
public class TableChangeEvent {

    public enum EventType { INSERT, UPDATE, DELETE }

    private String database;
    private String table;
    private EventType eventType;
    /** 变更后的列数据（DELETE 时为删除前的数据） */
    private Map<String, String> data;
    /** UPDATE 时变更前的列数据 */
    private Map<String, String> oldData;
}
