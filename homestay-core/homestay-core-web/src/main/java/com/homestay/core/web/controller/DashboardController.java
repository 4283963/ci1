package com.homestay.core.web.controller;

import com.homestay.core.common.result.R;
import com.homestay.core.model.vo.ChannelOrderDistributionVO;
import com.homestay.core.model.vo.RealtimeRoomStatusVO;
import com.homestay.core.model.vo.RoomTypeHeatmapVO;
import com.homestay.core.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/realtime")
    public R<List<RealtimeRoomStatusVO>> getRealtimeRoomStatus() {
        return R.ok(dashboardService.getRealtimeRoomStatus());
    }

    @GetMapping("/channel-distribution")
    public R<List<ChannelOrderDistributionVO>> getChannelDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate s = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate e = endDate != null ? endDate : LocalDate.now();
        return R.ok(dashboardService.getChannelOrderDistribution(s, e));
    }

    @GetMapping("/heatmap")
    public R<List<RoomTypeHeatmapVO>> getHeatmap(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate s = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate e = endDate != null ? endDate : LocalDate.now().plusDays(30);
        return R.ok(dashboardService.getRoomTypeHeatmap(s, e));
    }

    @Scheduled(fixedRate = 30000)
    public void pushRealtimeData() {
        try {
            List<RealtimeRoomStatusVO> data = dashboardService.getRealtimeRoomStatus();
            messagingTemplate.convertAndSend("/topic/realtime-rooms", data);
        } catch (Exception e) {
            // ignore when no subscribers
        }
    }
}
