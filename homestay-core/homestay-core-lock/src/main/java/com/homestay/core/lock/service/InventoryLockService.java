package com.homestay.core.lock.service;

import com.homestay.core.model.dto.InventoryLockDTO;
import com.homestay.core.model.entity.InventoryLock;

import java.util.List;

public interface InventoryLockService {

    InventoryLock tryLock(InventoryLockDTO dto);

    boolean confirmLock(Long lockId);

    boolean releaseLock(Long lockId);

    boolean releaseLockBySource(String sourceType, String sourceId);

    InventoryLock getLockBySource(String sourceType, String sourceId);

    List<InventoryLock> getLocksByRoomType(Long roomTypeId, String startDate, String endDate);

    void cleanExpiredLocks();
}
