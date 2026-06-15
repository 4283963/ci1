package com.homestay.core.web.controller;

import com.homestay.core.common.result.R;
import com.homestay.core.model.dto.OrderCreateDTO;
import com.homestay.core.model.entity.Order;
import com.homestay.core.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public R<Order> createOrder(@RequestBody OrderCreateDTO dto) {
        return R.ok(orderService.createOrder(dto));
    }

    @GetMapping("/{id}")
    public R<Order> getOrder(@PathVariable Long id) {
        return R.ok(orderService.getOrderById(id));
    }

    @PostMapping("/{id}/confirm")
    public R<Order> confirmOrder(@PathVariable Long id) {
        return R.ok(orderService.confirmOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public R<Order> cancelOrder(@PathVariable Long id,
                                @RequestParam(required = false, defaultValue = "") String reason) {
        return R.ok(orderService.cancelOrder(id, reason));
    }

    @GetMapping("/list")
    public R<List<Order>> listOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer status) {
        return R.ok(orderService.listOrders(startDate, endDate, status));
    }

    @GetMapping("/channel")
    public R<Order> getByChannelOrderNo(@RequestParam Long channelId,
                                        @RequestParam String channelOrderNo) {
        return R.ok(orderService.getOrderByChannelOrderNo(channelId, channelOrderNo));
    }
}
