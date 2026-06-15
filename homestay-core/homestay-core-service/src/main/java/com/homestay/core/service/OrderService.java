package com.homestay.core.service;

import com.homestay.core.model.dto.OrderCreateDTO;
import com.homestay.core.model.entity.Order;
import com.homestay.core.model.vo.ChannelOrderDistributionVO;
import com.homestay.core.model.vo.RealtimeRoomStatusVO;
import com.homestay.core.model.vo.RoomTypeHeatmapVO;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {

    Order createOrder(OrderCreateDTO dto);

    Order confirmOrder(Long orderId);

    Order cancelOrder(Long orderId, String reason);

    Order getOrderById(Long orderId);

    Order getOrderByChannelOrderNo(Long channelId, String channelOrderNo);

    List<Order> listOrders(LocalDate startDate, LocalDate endDate, Integer status);
}
