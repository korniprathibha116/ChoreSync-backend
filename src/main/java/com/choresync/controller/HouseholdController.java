package com.choresync.controller;

import com.choresync.dto.HouseholdDtos.HouseholdRequest;
import com.choresync.dto.HouseholdDtos.HouseholdResponse;
import com.choresync.dto.HouseholdDtos.HouseholdSummaryResponse;
import com.choresync.service.HouseholdService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/households")
public class HouseholdController {

    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @GetMapping
    public ResponseEntity<List<HouseholdResponse>> getHouseholds() {
        return ResponseEntity.ok(householdService.getHouseholdsForCurrentUser());
    }

    @PostMapping
    public ResponseEntity<HouseholdResponse> createHousehold(@RequestBody HouseholdRequest request) {
        HouseholdResponse created = householdService.createHousehold(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HouseholdSummaryResponse> getHouseholdSummary(@PathVariable Long id) {
        return ResponseEntity.ok(householdService.getHouseholdSummary(id));
    }

    @PostMapping("/join")
    public ResponseEntity<HouseholdResponse> joinHousehold(@RequestBody Map<String, String> body) {
        String inviteCode = body.get("inviteCode");
        HouseholdResponse joined = householdService.joinHousehold(inviteCode);
        return ResponseEntity.ok(joined);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        householdService.removeMember(id, userId);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully."));
    }
}
