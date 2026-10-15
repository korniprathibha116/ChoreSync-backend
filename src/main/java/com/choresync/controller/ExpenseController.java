package com.choresync.controller;

import com.choresync.dto.ExpenseDtos.ExpenseRequest;
import com.choresync.dto.ExpenseDtos.ExpenseResponse;
import com.choresync.dto.ExpenseDtos.ExpenseSummaryResponse;
import com.choresync.service.ExpenseService;
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
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/households/{householdId}/expenses")
    public ResponseEntity<ExpenseSummaryResponse> getExpenses(@PathVariable Long householdId) {
        return ResponseEntity.ok(expenseService.getExpensesSummary(householdId));
    }

    @PostMapping("/households/{householdId}/expenses")
    public ResponseEntity<ExpenseResponse> createExpense(
        @PathVariable Long householdId,
        @RequestBody ExpenseRequest request
    ) {
        ExpenseResponse created = expenseService.createExpense(householdId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
        @PathVariable Long id,
        @RequestBody ExpenseRequest request
    ) {
        return ResponseEntity.ok(expenseService.updateExpense(id, request));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(Map.of("message", "Expense deleted successfully."));
    }
}
