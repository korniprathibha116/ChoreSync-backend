package com.choresync.controller;

import com.choresync.service.ReportService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/households/{householdId}/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getReports(
        @PathVariable Long householdId,
        @RequestParam(required = false, defaultValue = "Last 30 Days") String timeRange
    ) {
        return ResponseEntity.ok(reportService.getReports(householdId, timeRange));
    }
}
