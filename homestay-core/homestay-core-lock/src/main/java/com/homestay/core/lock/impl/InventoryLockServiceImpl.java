package com.homestay.core.lock.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.core.common.enums.LockTypeEnum;
import com.homestay.core.common.exception.BusinessException;
import com.homestay.core.common.exception.InventoryInsufficientException;
import com.homestay.core.common.utils.DateUtils;
import com.homestay.core.lock.annotation.DistributedLock;
import com.homestay.core.lock.service.InventoryLockService;
import com.homestay.core.lock.service.InventoryService;
import com.homestay.core.model.dto.InventoryLockDTO;
import com.homestay.core.model.entity.Inventory;
import com.homestay.core.model.entity.InventoryLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryLockServiceImpl implements InventoryLockService {

    private final RedissonClient redissonClient;
    private final InventoryService inventoryService;
    private final BaseMapper<InventoryLock> lockMapper;
    private final BaseMapper<Inventory> inventoryMapper;

    private static final int MAX_RETRY = 3;
    private static final long RETRY_BASE_MS = 50;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "#dto.roomTypeId",
                     prefix = "HOMESTAY:INV:LOCK",
                     waitTime = 10, leaseTime = 120)
    public InventoryLock tryLock(InventoryLockDTO dto) {
        return executeWithRetry("tryLock", () -> doTryLock(dto));
    }

    private InventoryLock doTryLock(InventoryLockDTO dto) {
        log.info("开始尝试库存锁定: roomTypeId={}, start={}, end={}, count={}, source={}",
                dto.getRoomTypeId(), dto.getStartDate(), dto.getEndDate(),
                dto.getLockCount(), dto.getSourceType());

        List<LocalDate> dateRange = DateUtils.getDateRange(dto.getStartDate(), dto.getEndDate());
        Collections.sort(dateRange);

        String lockKey = DateUtils.generateLockKey(
                dto.getRoomTypeId(), dto.getStartDate(), dto.getEndDate(),
                dto.getSourceType(), dto.getSourceId()
        );

        LambdaQueryWrapper<InventoryLock> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(InventoryLock::getLockKey, lockKey);
        InventoryLock existing = lockMapper.selectOne(existingWrapper);
        if (existing != null && existing.getStatus() == 1) {
            log.warn("库存锁已存在，直接返回: lockKey={}", lockKey);
            return existing;
        }

        List<Inventory> inventories = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getRoomTypeId, dto.getRoomTypeId())
                        .ge(Inventory::getStayDate, dto.getStartDate())
                        .le(Inventory::getStayDate, dto.getEndDate())
                        .orderByAsc(Inventory::getStayDate)
                        .last("FOR UPDATE")
        );

        if (inventories.size() != dateRange.size()) {
            for (LocalDate date : dateRange) {
                boolean found = inventories.stream().anyMatch(inv -> inv.getStayDate().equals(date));
                if (!found) {
                    throw BusinessException.of(String.format("房型[%d]在[%s]的库存未初始化", dto.getRoomTypeId(), date));
                }
            }
        }

        for (Inventory inv : inventories) {
            int available = inv.getTotalRooms() - inv.getBookedRooms() - inv.getLockedRooms();
            if (available < dto.getLockCount()) {
                throw InventoryInsufficientException.of(
                        dto.getRoomTypeId(), inv.getStayDate().toString(),
                        dto.getLockCount(), available);
            }
        }

        for (Inventory inv : inventories) {
            int updated = inventoryMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Inventory>()
                            .eq(Inventory::getId, inv.getId())
                            .ge(Inventory::getAvailableRooms, dto.getLockCount())
                            .setSql("locked_rooms = locked_rooms + " + dto.getLockCount())
                            .setSql("available_rooms = available_rooms - " + dto.getLockCount())
                            .setSql("version = version + 1")
                            .set(Inventory::getUpdatedAt, LocalDateTime.now())
            );
            if (updated <= 0) {
                throw new RuntimeException("库存锁定失败，存在并发冲突，请重试");
            }
        }

        InventoryLock lock = new InventoryLock();
        lock.setLockKey(lockKey);
        lock.setRoomTypeId(dto.getRoomTypeId());
        lock.setStartDate(dto.getStartDate());
        lock.setEndDate(dto.getEndDate());
        lock.setLockCount(dto.getLockCount());
        lock.setLockType(dto.getLockType() != null ? dto.getLockType() : LockTypeEnum.PRE_LOCK.getCode());
        lock.setSourceType(dto.getSourceType());
        lock.setSourceId(dto.getSourceId());
        lock.setChannelId(dto.getChannelId());
        int expire = dto.getExpireMinutes() != null ? dto.getExpireMinutes() : 30;
        lock.setExpireAt(LocalDateTime.now().plusMinutes(expire));
        lock.setLockedAt(LocalDateTime.now());
        lock.setStatus(1);
        lock.setVersion(0L);
        lock.setCreatedAt(LocalDateTime.now());
        lock.setUpdatedAt(LocalDateTime.now());
        lockMapper.insert(lock);

        RLock redissonLock = redissonClient.getLock("LOCK:RECORD:" + lock.getId());
        redissonLock.expire(expire, TimeUnit.MINUTES);

        log.info("库存锁定成功: lockId={}, lockKey={}, 影响天数={}", lock.getId(), lockKey, dateRange.size());
        return lock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmLock(Long lockId) {
        return executeWithRetry("confirmLock", () -> {
            InventoryLock lock = lockMapper.selectById(lockId);
            if (lock == null) {
                throw BusinessException.of("库存锁不存在: " + lockId);
            }
            if (lock.getStatus() != 1) {
                throw BusinessException.of("库存锁状态异常，无法确认");
            }
            if (lock.getExpireAt().isBefore(LocalDateTime.now())) {
                releaseLock(lockId);
                throw BusinessException.of("库存锁已过期");
            }

            lock.setLockType(LockTypeEnum.CONFIRM_LOCK.getCode());
            lock.setExpireAt(lock.getEndDate().atStartOfDay().plusDays(1));
            lock.setUpdatedAt(LocalDateTime.now());
            lockMapper.updateById(lock);
            log.info("库存锁确认成功: lockId={}", lockId);
            return true;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseLock(Long lockId) {
        return executeWithRetry("releaseLock", () -> doReleaseLock(lockId));
    }

    private boolean doReleaseLock(Long lockId) {
        InventoryLock lock = lockMapper.selectById(lockId);
        if (lock == null || lock.getStatus() != 1) {
            return true;
        }

        List<LocalDate> dateRange = DateUtils.getDateRange(lock.getStartDate(), lock.getEndDate());
        Collections.sort(dateRange);

        List<Inventory> inventories = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getRoomTypeId(), lock.getRoomTypeId())
                        .ge(Inventory::getStayDate, lock.getStartDate())
                        .le(Inventory::getStayDate, lock.getEndDate())
                        .orderByAsc(Inventory::getStayDate)
                        .last("FOR UPDATE")
        );

        for (Inventory inv : inventories) {
            int releaseCount = Math.min(lock.getLockCount(), inv.getLockedRooms());
            if (releaseCount <= 0) continue;

            inventoryMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Inventory>()
                            .eq(Inventory::getId, inv.getId())
                            .ge(Inventory::getLockedRooms, releaseCount)
                            .setSql("locked_rooms = locked_rooms - " + releaseCount)
                            .setSql("available_rooms = available_rooms + " + releaseCount)
                            .setSql("version = version + 1")
                            .set(Inventory::getUpdatedAt, LocalDateTime.now())
            );
        }

        lock.setStatus(2);
        lock.setUnlockAt(LocalDateTime.now());
        lock.setUpdatedAt(LocalDateTime.now());
        lockMapper.updateById(lock);
        log.info("库存锁释放成功: lockId={}, 释放天数={}", lockId, dateRange.size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseLockBySource(String sourceType, String sourceId) {
        LambdaQueryWrapper<InventoryLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryLock::getSourceType, sourceType)
                .eq(InventoryLock::getSourceId, sourceId)
                .eq(InventoryLock::getStatus, 1);
        List<InventoryLock> locks = lockMapper.selectList(wrapper);
        locks.sort(Comparator.comparing(InventoryLock::getId));
        for (InventoryLock lock : locks) {
            releaseLock(lock.getId());
        }
        return true;
    }

    @Override
    public InventoryLock getLockBySource(String sourceType, String sourceId) {
        LambdaQueryWrapper<InventoryLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryLock::getSourceType, sourceType)
                .eq(InventoryLock::getSourceId, sourceId)
                .orderByDesc(InventoryLock::getCreatedAt)
                .last("LIMIT 1");
        return lockMapper.selectOne(wrapper);
    }

    @Override
    public List<InventoryLock> getLocksByRoomType(Long roomTypeId, String startDate, String endDate) {
        LambdaQueryWrapper<InventoryLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryLock::getRoomTypeId, roomTypeId)
                .eq(InventoryLock::getStatus, 1)
                .ge(InventoryLock::getEndDate, LocalDate.parse(startDate))
                .le(InventoryLock::getStartDate, LocalDate.parse(endDate));
        return lockMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanExpiredLocks() {
        log.info("开始清理过期库存锁");
        LambdaQueryWrapper<InventoryLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryLock::getStatus, 1)
                .lt(InventoryLock::getExpireAt, LocalDateTime.now())
                .orderByAsc(InventoryLock::getId);
        List<InventoryLock> expiredLocks = lockMapper.selectList(wrapper);
        int success = 0;
        for (InventoryLock lock : expiredLocks) {
            try {
                releaseLock(lock.getId());
                success++;
            } catch (Exception e) {
                log.error("清理过期锁失败: lockId={}", lock.getId(), e);
            }
        }
        log.info("清理过期库存锁完成，总数={}, 成功={}", expiredLocks.size(), success);
    }

    @FunctionalInterface
    private interface RetryCallable<T> {
        T call() throws Exception;
    }

    @FunctionalInterface
    private interface RetryRunnable {
        void run() throws Exception;
    }

    private <T> T executeWithRetry(String operationName, RetryCallable<T> action) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return action.call();
            } catch (DeadlockLoserDataAccessException | CannotAcquireLockException e) {
                lastException = e;
                log.warn("数据库锁冲突，第{}次重试: operation={}, reason={}", attempt, operationName, e.getMessage());
                sleepBackoff(attempt);
            } catch (RuntimeException e) {
                if (isDeadlockRelated(e)) {
                    lastException = e;
                    log.warn("检测到死锁相关异常，第{}次重试: operation={}", attempt, operationName);
                    sleepBackoff(attempt);
                } else {
                    throw e;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        log.error("重试{}次后仍然失败: operation={}", MAX_RETRY, operationName, lastException);
        throw new RuntimeException("操作失败，重试" + MAX_RETRY + "次后仍然失败: " + operationName, lastException);
    }

    private boolean executeWithRetry(String operationName, RetryRunnable action) {
        executeWithRetry(operationName, () -> {
            action.run();
            return true;
        });
        return true;
    }

    private boolean isDeadlockRelated(Throwable t) {
        if (t == null) return false;
        String msg = t.getMessage();
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("deadlock")
                || lower.contains("lock")
                || lower.contains("dead lock")
                || lower.contains("could not obtain lock");
    }

    private void sleepBackoff(int attempt) {
        try {
            long sleepMs = RETRY_BASE_MS * (long) Math.pow(2, attempt - 1);
            Thread.sleep(sleepMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
