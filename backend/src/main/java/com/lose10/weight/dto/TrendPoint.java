package com.lose10.weight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrendPoint(LocalDate date, BigDecimal rollingAverageKg) {
}
