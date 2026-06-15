package com.homestay.core.web.controller;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.core.lock.service.InventoryLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.homestay.core.common.result.R;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final InventoryLockService inventoryLockService;

    @PostMapping("/clean-expired-locks")
    public R<Void> cleanExpiredLocks() {
        inventoryLockService.cleanExpiredLocks();
        return R.ok();
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void autoCleanExpiredLocks() {
        inventoryLockService.cleanExpiredLocks();
    }
}
