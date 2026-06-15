package com.homestay.core.web.controller;

import com.homestay.core.common.result.R;
import com.homestay.core.lock.service.InventoryLockService;
import com.homestay.core.lock.service.InventoryService;
import com.homestay.core.model.dto.InventoryLockDTO;
import com.homestay.core.model.dto.InventoryQueryDTO;
import com.homestay.core.model.entity.InventoryLock;
import com.homestay.core.model.vo.InventoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryLockService inventoryLockService;

    @GetMapping("/query")
    public R<List<InventoryVO>> queryInventory(@RequestParam Long roomTypeId,
                                               @RequestParam LocalDate startDate,
                                               @RequestParam LocalDate endDate) {
        InventoryQueryDTO dto = new InventoryQueryDTO();
        dto.setRoomTypeId(roomTypeId);
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        return R.ok(inventoryService.queryInventory(dto));
    }

    @PostMapping("/lock")
    public R<InventoryLock> lockInventory(@RequestBody InventoryLockDTO dto) {
        return R.ok(inventoryLockService.tryLock(dto));
    }

    @PostMapping("/lock/{lockId}/confirm")
    public R<Boolean> confirmLock(@PathVariable Long lockId) {
        return R.ok(inventoryLockService.confirmLock(lockId));
    }

    @PostMapping("/lock/{lockId}/release")
    public R<Boolean> releaseLock(@PathVariable Long lockId) {
        return R.ok(inventoryLockService.releaseLock(lockId));
    }

    @PostMapping("/adjust")
    public R<Boolean> adjustInventory(@RequestParam Long roomTypeId,
                                      @RequestParam LocalDate date,
                                      @RequestParam Integer newTotal) {
        return R.ok(inventoryService.adjustInventory(roomTypeId, date, newTotal));
    }

    @PostMapping("/price")
    public R<Boolean> adjustPrice(@RequestParam Long roomTypeId,
                                  @RequestParam LocalDate date,
                                  @RequestParam BigDecimal price) {
        return R.ok(inventoryService.adjustPrice(roomTypeId, date, price));
    }

    @PostMapping("/generate")
    public R<Void> generateInventory(@RequestParam Long roomTypeId,
                                     @RequestParam LocalDate startDate,
                                     @RequestParam LocalDate endDate,
                                     @RequestParam(required = false, defaultValue = "1") Integer rooms,
                                     @RequestParam(required = false, defaultValue = "0") BigDecimal price) {
        inventoryService.generateInventory(roomTypeId, startDate, endDate, rooms, price);
        return R.ok();
    }
}
