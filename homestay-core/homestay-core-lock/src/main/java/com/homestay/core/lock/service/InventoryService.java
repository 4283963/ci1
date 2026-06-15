package com.homestay.core.lock.service;

import com.homestay.core.model.dto.InventoryLockDTO;
import com.homestay.core.model.dto.InventoryQueryDTO;
import com.homestay.core.model.entity.Inventory;
import com.homestay.core.model.vo.InventoryVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InventoryService {

    List<InventoryVO> queryInventory(InventoryQueryDTO queryDTO);

    Inventory getInventory(Long roomTypeId, LocalDate date);

    boolean deductInventory(Long roomTypeId, LocalDate date, int count, String sourceType, String sourceId);

    boolean restoreInventory(Long roomTypeId, LocalDate date, int count, String sourceType, String sourceId);

    boolean adjustInventory(Long roomTypeId, LocalDate date, int newTotal);

    boolean adjustPrice(Long roomTypeId, LocalDate date, BigDecimal newPrice);

    void generateInventory(Long roomTypeId, LocalDate startDate, LocalDate endDate, Integer defaultRooms, BigDecimal defaultPrice);

    List<LocalDate> checkAvailability(Long roomTypeId, LocalDate startDate, LocalDate endDate, int requiredRooms);
}
