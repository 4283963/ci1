package com.homestay.core.web.controller;

import com.homestay.core.common.result.R;
import com.homestay.core.model.vo.RevenueAnalysisVO;
import com.homestay.core.service.RevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    @GetMapping("/analysis")
    public R<RevenueAnalysisVO> getRevenueAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate s = startDate != null ? startDate : LocalDate.now();
        LocalDate e = endDate != null ? endDate : LocalDate.now().plusDays(30);
        return R.ok(revenueService.getRevenueAnalysis(s, e));
    }
}
