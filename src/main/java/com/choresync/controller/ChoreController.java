package com.choresync.controller;

import com.choresync.dto.ChoreDtos.ChoreRequest;
import com.choresync.dto.ChoreDtos.ChoreResponse;
import com.choresync.dto.ChoreDtos.CompletionRequest;
import com.choresync.service.ChoreService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChoreController {

    private final ChoreService choreService;

    public ChoreController(ChoreService choreService) {
        this.choreService = choreService;
    }

    @GetMapping("/households/{householdId}/chores")
    public ResponseEntity<List<ChoreResponse>> getChores(@PathVariable Long householdId) {
        return ResponseEntity.ok(choreService.getChoresForHousehold(householdId));
    }

    @PostMapping("/households/{householdId}/chores")
    public ResponseEntity<ChoreResponse> createChore(
        @PathVariable Long householdId,
        @RequestBody ChoreRequest request
    ) {
        ChoreResponse created = choreService.createChore(householdId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/chores/{id}")
    public ResponseEntity<ChoreResponse> updateChore(
        @PathVariable Long id,
        @RequestBody ChoreRequest request
    ) {
        return ResponseEntity.ok(choreService.updateChore(id, request));
    }

    @DeleteMapping("/chores/{id}")
    public ResponseEntity<?> deleteChore(@PathVariable Long id) {
        choreService.deleteChore(id);
        return ResponseEntity.ok(Map.of("message", "Chore deleted successfully."));
    }

    @PostMapping("/chores/{id}/complete")
    public ResponseEntity<ChoreResponse> completeChore(
        @PathVariable Long id,
        @RequestBody(required = false) CompletionRequest request
    ) {
        String notes = request != null ? request.getNotes() : null;
        return ResponseEntity.ok(choreService.completeChore(id, notes));
    }

    @PostMapping("/chores/{id}/reopen")
    public ResponseEntity<ChoreResponse> reopenChore(@PathVariable Long id) {
        return ResponseEntity.ok(choreService.reopenChore(id));
    }
}
