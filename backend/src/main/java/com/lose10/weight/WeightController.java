package com.lose10.weight;

import com.lose10.platform.security.CurrentUser;
import com.lose10.weight.dto.LogWeightRequest;
import com.lose10.weight.dto.WeightEntryResponse;
import com.lose10.weight.dto.WeightTrendResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/weight")
public class WeightController {

    private final WeightService weightService;

    public WeightController(WeightService weightService) {
        this.weightService = weightService;
    }

    @PostMapping
    public ResponseEntity<WeightEntryResponse> log(@Valid @RequestBody LogWeightRequest request) {
        return ResponseEntity.status(201).body(weightService.logWeight(CurrentUser.id(), request));
    }

    @GetMapping
    public ResponseEntity<Page<WeightEntryResponse>> history(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now();
        return ResponseEntity.ok(weightService.history(CurrentUser.id(), effectiveFrom, effectiveTo, PageRequest.of(page, Math.min(size, 100))));
    }

    @GetMapping("/trend")
    public ResponseEntity<WeightTrendResponse> trend() {
        return ResponseEntity.ok(weightService.trend(CurrentUser.id()));
    }
}
