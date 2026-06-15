package com.homestay.core.lock.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.core.common.exception.BusinessException;
import com.homestay.core.common.utils.DateUtils;
import com.homestay.core.lock.service.InventoryService;
import com.homestay.core.model.dto.InventoryQueryDTO;
import com.homestay.core.model.entity.Inventory;
import com.homestay.core.model.vo.InventoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final BaseMapper<Inventory> inventoryMapper;

    private static final int MAX_RETRY = 3;
    private static final long RETRY_BASE_MS = 50;

    @Override
    public List<InventoryVO> queryInventory(InventoryQueryDTO queryDTO) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getRoomTypeId, queryDTO.getRoomTypeId())
                .ge(Inventory::getStayDate, queryDTO.getStartDate())
                .le(Inventory::getStayDate, queryDTO.getEndDate())
                .orderByAsc(Inventory::getStayDate);
        return inventoryMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Inventory getInventory(Long roomTypeId, LocalDate date) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getRoomTypeId, roomTypeId)
                .eq(Inventory::getStayDate, date);
        return inventoryMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductInventory(Long roomTypeId, LocalDate date, int count, String sourceType, String sourceId) {
        return executeWithRetry("deductInventory", () -> {
            int updated = inventoryMapper.update(null, new LambdaUpdateWrapper<Inventory>()
                    .eq(Inventory::getRoomTypeId, roomTypeId)
                    .eq(Inventory::getStayDate, date)
                    .ge(Inventory::getAvailableRooms, count)
                    .setSql("booked_rooms = booked_rooms + " + count)
                    .setSql("available_rooms = available_rooms - " + count)
                    .setSql("version = version + 1")
                    .set(Inventory::getUpdatedAt, LocalDateTime.now())
            );
            if (updated <= 0) {
                Inventory inv = getInventory(roomTypeId, date);
                if (inv == null) {
                    throw BusinessException.of(String.format("库存不存在: roomTypeId=%d, date=%s", roomTypeId, date));
                }
                throw BusinessException.of(String.format("可用库存不足: 可用%d, 需要%d", inv.getAvailableRooms(), count));
            }
            log.debug("库存扣减成功: roomTypeId={}, date={}, count={}, source={}", roomTypeId, date, count, sourceType);
            return true;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreInventory(Long roomTypeId, LocalDate date, int count, String sourceType, String sourceId) {
        return executeWithRetry("restoreInventory", () -> {
            Inventory inv = getInventory(roomTypeId, date);
            if (inv == null) {
                return true;
            }

            int restoreCount = Math.min(count, inv.getBookedRooms());
            if (restoreCount <= 0) {
                return true;
            }

            int updated = inventoryMapper.update(null, new LambdaUpdateWrapper<Inventory>()
                    .eq(Inventory::getId, inv.getId())
                    .ge(Inventory::getBookedRooms, restoreCount)
                    .setSql("booked_rooms = booked_rooms - " + restoreCount)
                    .setSql("available_rooms = available_rooms + " + restoreCount)
                    .setSql("version = version + 1")
                    .set(Inventory::getUpdatedAt, LocalDateTime.now())
            );
            if (updated > 0) {
                log.debug("库存回补成功: roomTypeId={}, date={}, count={}", roomTypeId, date, restoreCount);
            }
            return true;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adjustInventory(Long roomTypeId, LocalDate date, int newTotal) {
        return executeWithRetry("adjustInventory", () -> {
            Inventory inv = getInventory(roomTypeId, date);
            if (inv == null) {
                Inventory newInv = new Inventory();
                newInv.setRoomTypeId(roomTypeId);
                newInv.setStayDate(date);
                newInv.setTotalRooms(newTotal);
                newInv.setBookedRooms(0);
                newInv.setLockedRooms(0);
                newInv.setAvailableRooms(newTotal);
                newInv.setBasePrice(BigDecimal.ZERO);
                newInv.setVersion(0L);
                newInv.setCreatedAt(LocalDateTime.now());
                newInv.setUpdatedAt(LocalDateTime.now());
                inventoryMapper.insert(newInv);
                return true;
            }

            int currentOccupied = inv.getBookedRooms() + inv.getLockedRooms();
            if (newTotal < currentOccupied) {
                throw BusinessException.of(String.format("调整后的库存%d小于当前占用%d", newTotal, currentOccupied));
            }

            inventoryMapper.update(null, new LambdaUpdateWrapper<Inventory>()
                    .eq(Inventory::getId, inv.getId())
                    .set(Inventory::getTotalRooms, newTotal)
                    .set(Inventory::getAvailableRooms, newTotal - currentOccupied)
                    .setSql("version = version + 1")
                    .set(Inventory::getUpdatedAt, LocalDateTime.now())
            );
            return true;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adjustPrice(Long roomTypeId, LocalDate date, BigDecimal newPrice) {
        inventoryMapper.update(null, new LambdaUpdateWrapper<Inventory>()
                .eq(Inventory::getRoomTypeId, roomTypeId)
                .eq(Inventory::getStayDate, date)
                .set(Inventory::getBasePrice, newPrice)
                .set(Inventory::getUpdatedAt, LocalDateTime.now())
        );
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateInventory(Long roomTypeId, LocalDate startDate, LocalDate endDate,
                                  Integer defaultRooms, BigDecimal defaultPrice) {
        List<LocalDate> dates = DateUtils.getDateRange(startDate, endDate);
        int created = 0;
        for (LocalDate date : dates) {
            Inventory inv = getInventory(roomTypeId, date);
            if (inv == null) {
                Inventory newInv = new Inventory();
                newInv.setRoomTypeId(roomTypeId);
                newInv.setStayDate(date);
                newInv.setTotalRooms(defaultRooms);
                newInv.setBookedRooms(0);
                newInv.setLockedRooms(0);
                newInv.setAvailableRooms(defaultRooms);
                newInv.setBasePrice(defaultPrice);
                newInv.setVersion(0L);
                newInv.setCreatedAt(LocalDateTime.now());
                newInv.setUpdatedAt(LocalDateTime.now());
                inventoryMapper.insert(newInv);
                created++;
            }
        }
        log.info("库存批量生成完成: roomTypeId={}, 总数={}, 新增={}", roomTypeId, dates.size(), created);
    }

    @Override
    public List<LocalDate> checkAvailability(Long roomTypeId, LocalDate startDate, LocalDate endDate, int requiredRooms) {
        List<LocalDate> dateRange = DateUtils.getDateRange(startDate, endDate);
        List<LocalDate> insufficientDates = new ArrayList<>();
        for (LocalDate date : dateRange) {
            Inventory inv = getInventory(roomTypeId, date);
            if (inv == null || inv.getAvailableRooms() < requiredRooms) {
                insufficientDates.add(date);
            }
        }
        return insufficientDates;
    }

    private InventoryVO toVO(Inventory inv) {
        InventoryVO vo = new InventoryVO();
        BeanUtils.copyProperties(inv, vo);
        return vo;
    }

    @FunctionalInterface
    private interface RetryCallable<T> {
        T call() throws Exception;
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

    private boolean isDeadlockRelated(Throwable t) {
        if (t == null) return false;
        String msg = t.getMessage();
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("deadlock")
                || lower.contains("dead lock")
                || lower.contains("could not obtain lock")
                || lower.contains("lock wait timeout");
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
