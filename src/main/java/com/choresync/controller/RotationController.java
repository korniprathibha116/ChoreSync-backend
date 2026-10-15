package com.choresync.controller;

import com.choresync.service.RotationService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/households/{householdId}/rotation")
public class RotationController {

    private final RotationService rotationService;

    public RotationController(RotationService rotationService) {
        this.rotationService = rotationService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRotation(@PathVariable Long householdId) {
        return ResponseEntity.ok(rotationService.getRotationOverview(householdId));
    }

    @PostMapping("/rotate")
    public ResponseEntity<Map<String, Object>> executeRotation(@PathVariable Long householdId) {
        return ResponseEntity.ok(rotationService.executeRotation(householdId));
    }
}
