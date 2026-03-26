package com.example.business.canal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Canal 连接配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "canal")
public class CanalProperties {

    private Server server = new Server();
    private String destination = "example";
    private String subscribe = ".*\\..*";
    private int batchSize = 1000;
    private Migration migration = new Migration();
    /** 需要监听缓存同步的表名列表，为空则监听所有表 */
    private List<String> syncTables = List.of();
    /** 需要做数据迁移的表名列表，为空则迁移所有表 */
    private List<String> migrationTables = List.of();

    @Data
    public static class Server {
        private String host = "localhost";
        private int port = 11111;
    }

    @Data
    public static class Migration {
        private boolean enabled = false;
        private String targetUrl;
        private String targetUsername;
        private String targetPassword;
    }

    /**
     * 判断指定表是否在缓存同步监听范围内
     */
    public boolean isSyncTable(String table) {
        return syncTables.isEmpty() || syncTables.contains(table);
    }

    /**
     * 判断指定表是否在数据迁移范围内
     */
    public boolean isMigrationTable(String table) {
        return migrationTables.isEmpty() || migrationTables.contains(table);
    }
}

