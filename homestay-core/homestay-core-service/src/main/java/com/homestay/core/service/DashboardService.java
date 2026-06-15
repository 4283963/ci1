package com.homestay.core.service;

import com.homestay.core.model.vo.ChannelOrderDistributionVO;
import com.homestay.core.model.vo.RealtimeRoomStatusVO;
import com.homestay.core.model.vo.RoomTypeHeatmapVO;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {

    List<RealtimeRoomStatusVO> getRealtimeRoomStatus();

    List<ChannelOrderDistributionVO> getChannelOrderDistribution(LocalDate startDate, LocalDate endDate);

    List<RoomTypeHeatmapVO> getRoomTypeHeatmap(LocalDate startDate, LocalDate endDate);
}
