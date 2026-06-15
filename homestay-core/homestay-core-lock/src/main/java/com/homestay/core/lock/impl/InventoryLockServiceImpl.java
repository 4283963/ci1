package com.homestay.core.lock.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.core.common.enums.InventoryChangeTypeEnum;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "#dto.roomTypeId + '_' + #dto.startDate + '_' + #dto.endDate",
                     prefix = "HOMESTAY:INV:LOCK",
                     waitTime = 5, leaseTime = 60)
    public InventoryLock tryLock(InventoryLockDTO dto) {
        log.info("开始尝试库存锁定: roomTypeId={}, start={}, end={}, count={}",
                dto.getRoomTypeId(), dto.getStartDate(), dto.getEndDate(), dto.getLockCount());

        List<LocalDate> dateRange = DateUtils.getDateRange(dto.getStartDate(), dto.getEndDate());
        for (LocalDate date : dateRange) {
            Inventory inv = inventoryService.getInventory(dto.getRoomTypeId(), date);
            if (inv == null) {
                throw BusinessException.of(String.format("房型[%d]在[%s]的库存未初始化", dto.getRoomTypeId(), date));
            }
            int available = inv.getTotalRooms() - inv.getBookedRooms() - inv.getLockedRooms();
            if (available < dto.getLockCount()) {
                throw InventoryInsufficientException.of(dto.getRoomTypeId(), date.toString(), dto.getLockCount(), available);
            }
        }

        String lockKey = DateUtils.generateLockKey(
                dto.getRoomTypeId(), dto.getStartDate(), dto.getEndDate(),
                dto.getSourceType(), dto.getSourceId()
        );

        LambdaQueryWrapper<InventoryLock> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(InventoryLock::getLockKey, lockKey);
        InventoryLock existing = lockMapper.selectOne(existingWrapper);
        if (existing != null && existing.getStatus() == 1) {
            log.warn("库存锁已存在: lockKey={}", lockKey);
            return existing;
        }

        for (LocalDate date : dateRange) {
            int updated = inventoryMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Inventory>()
                    .eq(Inventory::getRoomTypeId, dto.getRoomTypeId())
                    .eq(Inventory::getStayDate, date)
                    .ge(Inventory::getAvailableRooms, dto.getLockCount())
                    .setSql("locked_rooms = locked_rooms + " + dto.getLockCount())
                    .setSql("available_rooms = available_rooms - " + dto.getLockCount())
                    .setSql("version = version + 1")
                    .set(Inventory::getUpdatedAt, LocalDateTime.now())
                    .eq(Inventory::getVersion, inventoryService.getInventory(dto.getRoomTypeId(), date).getVersion())
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

        log.info("库存锁定成功: lockId={}, lockKey={}", lock.getId(), lockKey);
        return lock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmLock(Long lockId) {
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseLock(Long lockId) {
        InventoryLock lock = lockMapper.selectById(lockId);
        if (lock == null || lock.getStatus() != 1) {
            return true;
        }

        List<LocalDate> dateRange = DateUtils.getDateRange(lock.getStartDate(), lock.getEndDate());
        for (LocalDate date : dateRange) {
            inventoryMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Inventory>()
                    .eq(Inventory::getRoomTypeId, lock.getRoomTypeId())
                    .eq(Inventory::getStayDate, date)
                    .ge(Inventory::getLockedRooms, lock.getLockCount())
                    .setSql("locked_rooms = locked_rooms - " + lock.getLockCount())
                    .setSql("available_rooms = available_rooms + " + lock.getLockCount())
                    .setSql("version = version + 1")
                    .set(Inventory::getUpdatedAt, LocalDateTime.now())
            );
        }

        lock.setStatus(2);
        lock.setUnlockAt(LocalDateTime.now());
        lock.setUpdatedAt(LocalDateTime.now());
        lockMapper.updateById(lock);
        log.info("库存锁释放成功: lockId={}", lockId);
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
                .lt(InventoryLock::getExpireAt, LocalDateTime.now());
        List<InventoryLock> expiredLocks = lockMapper.selectList(wrapper);
        for (InventoryLock lock : expiredLocks) {
            try {
                releaseLock(lock.getId());
            } catch (Exception e) {
                log.error("清理过期锁失败: lockId={}", lock.getId(), e);
            }
        }
        log.info("清理过期库存锁完成，数量={}", expiredLocks.size());
    }
}
