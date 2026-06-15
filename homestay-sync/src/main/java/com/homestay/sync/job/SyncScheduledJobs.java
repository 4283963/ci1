package com.homestay.sync.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.sync.client.HomestayCoreClient;
import com.homestay.sync.dto.ApiR;
import com.homestay.sync.entity.SyncLog;
import com.homestay.sync.service.OrderSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduledJobs {

    private final RedissonClient redissonClient;
    private final BaseMapper<SyncLog> syncLogMapper;
    private final HomestayCoreClient coreClient;
    private final OrderSyncService orderSyncService;

    @Scheduled(cron = "${sync.inventory.cron:0 0 */2 * * ?}")
    public void syncInventoryJob() {
        executeWithLock("SYNC_INVENTORY", () -> {
            log.info("========== 定时任务: 库存同步开始 ==========");
            SyncLog syncLog = createSyncLog("INVENTORY_SYNC", "库存全量同步", 1);
            try {
                syncLog.setTotalCount(0);
                syncLog.setSuccessCount(0);
                syncLog.setSyncStatus(2);
                log.info("库存同步完成（待对接各渠道实际API）");
            } catch (Exception e) {
                log.error("库存同步失败", e);
                syncLog.setSyncStatus(-1);
                syncLog.setErrorMsg(e.getMessage());
                syncLog.setFailCount(1);
            } finally {
                finishSyncLog(syncLog);
            }
        });
    }

    @Scheduled(cron = "${sync.order.cron:0 */5 * * * ?}")
    public void syncOrderJob() {
        executeWithLock("SYNC_ORDER", () -> {
            log.info("========== 定时任务: 订单拉取同步开始 ==========");
            SyncLog syncLog = createSyncLog("ORDER_SYNC", "订单增量同步", 3);
            try {
                LocalDate start = LocalDate.now().minusDays(7);
                LocalDate end = LocalDate.now().plusDays(1);
                log.info("拉取订单时间范围: {} ~ {}", start, end);
                syncLog.setTotalCount(0);
                syncLog.setSuccessCount(0);
                syncLog.setSyncStatus(2);
            } catch (Exception e) {
                log.error("订单同步失败", e);
                syncLog.setSyncStatus(-1);
                syncLog.setErrorMsg(e.getMessage());
                syncLog.setFailCount(1);
            } finally {
                finishSyncLog(syncLog);
            }
        });
    }

    @Scheduled(cron = "${sync.price.cron:0 0 */1 * * ?}")
    public void syncPriceJob() {
        executeWithLock("SYNC_PRICE", () -> {
            log.info("========== 定时任务: 价格同步开始 ==========");
            SyncLog syncLog = createSyncLog("PRICE_SYNC", "价格推送同步", 2);
            try {
                syncLog.setSyncStatus(2);
                syncLog.setSuccessCount(0);
            } catch (Exception e) {
                log.error("价格同步失败", e);
                syncLog.setSyncStatus(-1);
                syncLog.setErrorMsg(e.getMessage());
                syncLog.setFailCount(1);
            } finally {
                finishSyncLog(syncLog);
            }
        });
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void roomStatusSyncJob() {
        executeWithLock("SYNC_ROOM_STATUS", () -> {
            log.info("========== 定时任务: 房态同步开始 ==========");
            SyncLog syncLog = createSyncLog("ROOM_STATUS_SYNC", "房态同步", 4);
            try {
                syncLog.setSyncStatus(2);
            } catch (Exception e) {
                syncLog.setSyncStatus(-1);
                syncLog.setErrorMsg(e.getMessage());
            } finally {
                finishSyncLog(syncLog);
            }
        });
    }

    private void executeWithLock(String lockKey, Runnable task) {
        RLock lock = redissonClient.getLock("JOB:LOCK:" + lockKey);
        try {
            boolean locked = lock.tryLock(0, 30, TimeUnit.MINUTES);
            if (!locked) {
                log.warn("定时任务跳过，未获取到分布式锁: {}", lockKey);
                return;
            }
            task.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private SyncLog createSyncLog(String code, String name, int type) {
        SyncLog log = new SyncLog();
        log.setTaskCode(code);
        log.setTaskName(name);
        log.setTaskType(type);
        log.setStartTime(LocalDateTime.now());
        log.setSyncStatus(1);
        log.setCreatedAt(LocalDateTime.now());
        syncLogMapper.insert(log);
        return log;
    }

    private void finishSyncLog(SyncLog log) {
        log.setEndTime(LocalDateTime.now());
        if (log.getTotalCount() == null) log.setTotalCount(0);
        if (log.getSuccessCount() == null) log.setSuccessCount(0);
        if (log.getFailCount() == null) log.setFailCount(0);
        syncLogMapper.updateById(log);
    }
}
