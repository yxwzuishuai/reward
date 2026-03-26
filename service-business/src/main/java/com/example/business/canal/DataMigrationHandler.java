package com.example.business.canal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据迁移处理器
 * 将 Canal 捕获的 binlog 变更实时同步到目标数据库，实现增量迁移
 */
@Slf4j
@Component
public class DataMigrationHandler {

    private volatile DataSource targetDataSource;

    public void setTargetDataSource(DataSource targetDataSource) {
        this.targetDataSource = targetDataSource;
    }

    /**
     * 处理变更事件，将 DML 操作同步到目标库
     */
    public void handle(TableChangeEvent event) {
        if (targetDataSource == null) {
            return;
        }
        try {
            switch (event.getEventType()) {
                case INSERT -> replayInsert(event);
                case UPDATE -> replayUpdate(event);
                case DELETE -> replayDelete(event);
            }
        } catch (SQLException e) {
            log.error("数据迁移失败: table={}, type={}, error={}",
                    event.getTable(), event.getEventType(), e.getMessage(), e);
        }
    }

    private void replayInsert(TableChangeEvent event) throws SQLException {
        Map<String, String> data = event.getData();
        String columns = String.join(", ", data.keySet());
        String placeholders = data.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", event.getTable(), columns, placeholders);

        try (Connection conn = targetDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (String value : data.values()) {
                ps.setString(i++, value);
            }
            ps.executeUpdate();
            log.info("数据迁移 INSERT: table={}, columns={}", event.getTable(), columns);
        }
    }

    private void replayUpdate(TableChangeEvent event) throws SQLException {
        Map<String, String> data = event.getData();
        String id = data.get("id");
        if (id == null) {
            log.warn("数据迁移 UPDATE 跳过: 无主键 id, table={}", event.getTable());
            return;
        }
        String setClauses = data.entrySet().stream()
                .filter(e -> !"id".equals(e.getKey()))
                .map(e -> e.getKey() + " = ?")
                .collect(Collectors.joining(", "));
        String sql = String.format("UPDATE %s SET %s WHERE id = ?", event.getTable(), setClauses);

        try (Connection conn = targetDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (!"id".equals(entry.getKey())) {
                    ps.setString(i++, entry.getValue());
                }
            }
            ps.setString(i, id);
            ps.executeUpdate();
            log.info("数据迁移 UPDATE: table={}, id={}", event.getTable(), id);
        }
    }

    private void replayDelete(TableChangeEvent event) throws SQLException {
        String id = event.getData().get("id");
        if (id == null) {
            log.warn("数据迁移 DELETE 跳过: 无主键 id, table={}", event.getTable());
            return;
        }
        String sql = String.format("DELETE FROM %s WHERE id = ?", event.getTable());

        try (Connection conn = targetDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
            log.info("数据迁移 DELETE: table={}, id={}", event.getTable(), id);
        }
    }
}
