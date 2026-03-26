package com.example.business.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Canal 客户端
 * 连接 Canal Server，订阅 binlog 变更，分发给缓存同步和数据迁移处理器
 */
@Slf4j
@Component
public class CanalClient {

    private final CanalProperties properties;
    private final CacheSyncHandler cacheSyncHandler;
    private final DataMigrationHandler migrationHandler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;
    private CanalConnector connector;

    public CanalClient(CanalProperties properties,
                       CacheSyncHandler cacheSyncHandler,
                       DataMigrationHandler migrationHandler) {
        this.properties = properties;
        this.cacheSyncHandler = cacheSyncHandler;
        this.migrationHandler = migrationHandler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        initMigrationDataSource();

        workerThread = new Thread(this::process, "canal-worker");
        workerThread.setDaemon(true);
        workerThread.start();
        log.info("Canal 客户端启动: {}:{}, destination={}",
                properties.getServer().getHost(), properties.getServer().getPort(), properties.getDestination());
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (connector != null) {
            try { connector.disconnect(); } catch (Exception ignored) {}
        }
        log.info("Canal 客户端已停止");
    }

    private void initMigrationDataSource() {
        CanalProperties.Migration migration = properties.getMigration();
        if (migration.isEnabled() && migration.getTargetUrl() != null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(migration.getTargetUrl());
            config.setUsername(migration.getTargetUsername());
            config.setPassword(migration.getTargetPassword());
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(1);
            migrationHandler.setTargetDataSource(new HikariDataSource(config));
            log.info("数据迁移目标数据源已初始化: {}", migration.getTargetUrl());
        }
    }

    private void process() {
        while (running.get()) {
            try {
                connector = CanalConnectors.newSingleConnector(
                        new InetSocketAddress(properties.getServer().getHost(), properties.getServer().getPort()),
                        properties.getDestination(), "", "");
                connector.connect();
                connector.subscribe(properties.getSubscribe());
                connector.rollback();
                log.info("Canal 连接成功，开始监听 binlog...");

                while (running.get()) {
                    Message message = connector.getWithoutAck(properties.getBatchSize());
                    long batchId = message.getId();
                    int size = message.getEntries().size();

                    if (batchId != -1 && size > 0) {
                        processEntries(message.getEntries());
                    }
                    connector.ack(batchId);
                }
            } catch (Exception e) {
                log.error("Canal 处理异常，5秒后重连: {}", e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } finally {
                if (connector != null) {
                    try { connector.disconnect(); } catch (Exception ignored) {}
                }
            }
        }
    }

    private void processEntries(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN
                    || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }
            try {
                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                CanalEntry.EventType eventType = rowChange.getEventType();
                String database = entry.getHeader().getSchemaName();
                String table = entry.getHeader().getTableName();

                for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                    TableChangeEvent event = buildEvent(database, table, eventType, rowData);
                    if (event != null) {
                        // 缓存同步（按表过滤）
                        if (properties.isSyncTable(table)) {
                            cacheSyncHandler.handle(event);
                        }
                        // 数据迁移（按配置开关 + 表过滤）
                        if (properties.getMigration().isEnabled() && properties.isMigrationTable(table)) {
                            migrationHandler.handle(event);
                        }
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                log.error("解析 binlog entry 失败: {}", e.getMessage());
            }
        }
    }

    private TableChangeEvent buildEvent(String database, String table,
                                         CanalEntry.EventType eventType,
                                         CanalEntry.RowData rowData) {
        TableChangeEvent.EventType type = switch (eventType) {
            case INSERT -> TableChangeEvent.EventType.INSERT;
            case UPDATE -> TableChangeEvent.EventType.UPDATE;
            case DELETE -> TableChangeEvent.EventType.DELETE;
            default -> null;
        };
        if (type == null) return null;

        Map<String, String> data = new HashMap<>();
        Map<String, String> oldData = new HashMap<>();

        if (type == TableChangeEvent.EventType.DELETE) {
            rowData.getBeforeColumnsList().forEach(col -> data.put(col.getName(), col.getValue()));
        } else {
            rowData.getAfterColumnsList().forEach(col -> data.put(col.getName(), col.getValue()));
            if (type == TableChangeEvent.EventType.UPDATE) {
                rowData.getBeforeColumnsList().forEach(col -> oldData.put(col.getName(), col.getValue()));
            }
        }

        return TableChangeEvent.builder()
                .database(database)
                .table(table)
                .eventType(type)
                .data(data)
                .oldData(oldData)
                .build();
    }
}
