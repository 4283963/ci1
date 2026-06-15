package com.homestay.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.core.common.enums.*;
import com.homestay.core.common.exception.BusinessException;
import com.homestay.core.common.utils.DateUtils;
import com.homestay.core.lock.service.InventoryLockService;
import com.homestay.core.lock.service.InventoryService;
import com.homestay.core.model.dto.InventoryLockDTO;
import com.homestay.core.model.dto.OrderCreateDTO;
import com.homestay.core.model.entity.*;
import com.homestay.core.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final BaseMapper<Order> orderMapper;
    private final BaseMapper<OrderDaily> orderDailyMapper;
    private final InventoryLockService inventoryLockService;
    private final InventoryService inventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(OrderCreateDTO dto) {
        log.info("创建订单: channelOrderNo={}, roomType={}, {}~{}",
                dto.getChannelOrderNo(), dto.getRoomTypeId(), dto.getCheckinDate(), dto.getCheckoutDate());

        if (dto.getChannelId() != null && dto.getChannelOrderNo() != null) {
            Order exist = getOrderByChannelOrderNo(dto.getChannelId(), dto.getChannelOrderNo());
            if (exist != null) {
                log.warn("渠道订单已存在，跳过创建: channelOrderNo={}", dto.getChannelOrderNo());
                return exist;
            }
        }

        String orderNo = "HS" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        int nightCount = DateUtils.getNightCount(dto.getCheckinDate(), dto.getCheckoutDate());

        InventoryLockDTO lockDTO = new InventoryLockDTO();
        lockDTO.setRoomTypeId(dto.getRoomTypeId());
        lockDTO.setStartDate(dto.getCheckinDate());
        lockDTO.setEndDate(dto.getCheckoutDate().minusDays(1));
        lockDTO.setLockCount(1);
        lockDTO.setLockType(LockTypeEnum.CONFIRM_LOCK.getCode());
        lockDTO.setSourceType(dto.getChannelId() != null ? SourceTypeEnum.CHANNEL_ORDER.getCode() : SourceTypeEnum.DIRECT_ORDER.getCode());
        lockDTO.setSourceId(dto.getChannelOrderNo() != null ? dto.getChannelOrderNo() : orderNo);
        lockDTO.setChannelId(dto.getChannelId());
        lockDTO.setExpireMinutes(60 * 24 * 30);
        InventoryLock lock = inventoryLockService.tryLock(lockDTO);

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setChannelId(dto.getChannelId());
        order.setChannelOrderNo(dto.getChannelOrderNo());
        order.setPropertyId(dto.getPropertyId());
        order.setRoomTypeId(dto.getRoomTypeId());
        order.setGuestName(dto.getGuestName());
        order.setGuestPhone(dto.getGuestPhone());
        order.setCheckinDate(dto.getCheckinDate());
        order.setCheckoutDate(dto.getCheckoutDate());
        order.setNightCount(nightCount);
        order.setGuestCount(dto.getGuestCount());
        order.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setOrderStatus(OrderStatusEnum.CONFIRMED.getCode());
        order.setPayStatus(0);
        order.setLockId(lock.getId());
        order.setRemark(dto.getRemark());
        order.setExtraData(dto.getExtraData());
        order.setBookTime(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        List<LocalDate> stayDates = DateUtils.getDateRange(dto.getCheckinDate(), dto.getCheckoutDate().minusDays(1));
        BigDecimal avgPrice = nightCount > 0 && dto.getTotalAmount() != null
                ? dto.getTotalAmount().divide(BigDecimal.valueOf(nightCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        for (LocalDate date : stayDates) {
            OrderDaily daily = new OrderDaily();
            daily.setOrderId(order.getId());
            daily.setRoomTypeId(dto.getRoomTypeId());
            daily.setStayDate(date);
            daily.setRoomPrice(avgPrice);
            daily.setCreatedAt(LocalDateTime.now());
            orderDailyMapper.insert(daily);

            inventoryService.deductInventory(dto.getRoomTypeId(), date, 1, lockDTO.getSourceType(), orderNo);
        }

        inventoryLockService.confirmLock(lock.getId());
        log.info("订单创建成功: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order confirmOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.of("订单不存在: " + orderId);
        }
        order.setOrderStatus(OrderStatusEnum.CONFIRMED.getCode());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        if (order.getLockId() != null) {
            inventoryLockService.confirmLock(order.getLockId());
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order cancelOrder(Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.of("订单不存在: " + orderId);
        }
        if (order.getOrderStatus().equals(OrderStatusEnum.CANCELLED.getCode())) {
            return order;
        }

        List<LocalDate> stayDates = DateUtils.getDateRange(order.getCheckinDate(), order.getCheckoutDate().minusDays(1));
        for (LocalDate date : stayDates) {
            inventoryService.restoreInventory(order.getRoomTypeId(), date, 1,
                    SourceTypeEnum.CHANNEL_ORDER.getCode(), order.getOrderNo());
        }

        if (order.getLockId() != null) {
            inventoryLockService.releaseLock(order.getLockId());
        }

        order.setOrderStatus(OrderStatusEnum.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单取消成功: orderId={}, reason={}", orderId, reason);
        return order;
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    public Order getOrderByChannelOrderNo(Long channelId, String channelOrderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getChannelId, channelId)
                .eq(Order::getChannelOrderNo, channelOrderNo)
                .last("LIMIT 1");
        return orderMapper.selectOne(wrapper);
    }

    @Override
    public List<Order> listOrders(LocalDate startDate, LocalDate endDate, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Order::getCheckinDate, startDate)
                .le(Order::getCheckinDate, endDate);
        if (status != null) {
            wrapper.eq(Order::getOrderStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectList(wrapper);
    }
}
