package com.lose10.analytics;

import com.lose10.analytics.dto.BusinessMetricsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class BusinessMetricsQueryService {

    private final BusinessMetricsDailyRepository repository;

    public BusinessMetricsQueryService(BusinessMetricsDailyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public BusinessMetricsResponse latest() {
        return repository.findFirstByOrderByMetricDateDesc()
                .map(BusinessMetricsResponse::from)
                .orElseGet(() -> BusinessMetricsResponse.empty(LocalDate.now()));
    }
}
