package com.homestay.core.service;

import com.homestay.core.model.vo.RevenueAnalysisVO;

import java.time.LocalDate;

public interface RevenueService {

    RevenueAnalysisVO getRevenueAnalysis(LocalDate startDate, LocalDate endDate);
}
