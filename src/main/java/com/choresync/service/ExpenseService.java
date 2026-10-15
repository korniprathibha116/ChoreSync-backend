package com.choresync.service;

import com.choresync.dto.ExpenseDtos.ExpenseRequest;
import com.choresync.dto.ExpenseDtos.ExpenseResponse;
import com.choresync.dto.ExpenseDtos.ExpenseSummaryResponse;
import com.choresync.entity.Expense;
import com.choresync.entity.ExpenseShare;
import com.choresync.entity.Household;
import com.choresync.entity.HouseholdMember;
import com.choresync.entity.User;
import com.choresync.exception.BusinessException;
import com.choresync.repository.ExpenseRepository;
import com.choresync.repository.ExpenseShareRepository;
import com.choresync.repository.HouseholdMemberRepository;
import com.choresync.repository.HouseholdRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final AuthService authService;

    public ExpenseService(
        ExpenseRepository expenseRepository,
        ExpenseShareRepository expenseShareRepository,
        HouseholdRepository householdRepository,
        HouseholdMemberRepository householdMemberRepository,
        AuthService authService
    ) {
        this.expenseRepository = expenseRepository;
        this.expenseShareRepository = expenseShareRepository;
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getExpensesSummary(Long householdId) {
        User currentUser = authService.getCurrentUserOrThrow();
        Household household = householdRepository.findById(householdId)
            .orElseThrow(() -> new BusinessException("Household not found."));

        List<Expense> expenses = expenseRepository.findByHouseholdOrderByCreatedAtDesc(household);
        List<ExpenseResponse> responseList = new ArrayList<>();

        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal currentUserPaid = BigDecimal.ZERO;
        BigDecimal currentUserShare = BigDecimal.ZERO;

        List<HouseholdMember> members = householdMemberRepository.findByHousehold(household);
        int memberCount = Math.max(members.size(), 1);

        for (Expense exp : expenses) {
            BigDecimal amt = exp.getAmount() != null ? exp.getAmount() : BigDecimal.ZERO;
            totalExpenses = totalExpenses.add(amt);

            if (exp.getPaidBy() != null && exp.getPaidBy().getId().equals(currentUser.getId())) {
                currentUserPaid = currentUserPaid.add(amt);
            }

            BigDecimal perPersonShare = amt.divide(BigDecimal.valueOf(memberCount), 2, RoundingMode.HALF_UP);
            currentUserShare = currentUserShare.add(perPersonShare);

            ExpenseResponse r = new ExpenseResponse();
            r.setId(exp.getId());
            r.setTitle(exp.getTitle());
            r.setCategory(exp.getCategory());
            r.setAmount(amt);
            r.setPaidById(exp.getPaidBy() != null ? exp.getPaidBy().getId() : null);
            r.setPaidByName(exp.getPaidBy() != null ? exp.getPaidBy().getDisplayName() : "Unknown");
            r.setCreatedAt(exp.getCreatedAt());
            r.setSplitMethod("Equal Split");
            r.setSharePerPerson(perPersonShare);
            responseList.add(r);
        }

        BigDecimal currentUserBalance = currentUserPaid.subtract(currentUserShare);

        return new ExpenseSummaryResponse(totalExpenses, currentUserShare, currentUserBalance, responseList);
    }

    @Transactional
    public ExpenseResponse createExpense(Long householdId, ExpenseRequest request) {
        User currentUser = authService.getCurrentUserOrThrow();
        Household household = householdRepository.findById(householdId)
            .orElseThrow(() -> new BusinessException("Household not found."));

        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("Expense title is required.");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("A valid positive expense amount is required.");
        }

        String category = request.getCategory() != null && !request.getCategory().isBlank() ? request.getCategory() : "General";

        Expense expense = new Expense(household, currentUser, request.getTitle().trim(), category, request.getAmount());
        expense = expenseRepository.save(expense);

        List<HouseholdMember> members = householdMemberRepository.findByHousehold(household);
        int count = Math.max(members.size(), 1);
        BigDecimal shareAmount = request.getAmount().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        for (HouseholdMember m : members) {
            ExpenseShare share = new ExpenseShare(expense, m.getUser(), shareAmount);
            expenseShareRepository.save(share);
        }

        ExpenseResponse resp = new ExpenseResponse();
        resp.setId(expense.getId());
        resp.setTitle(expense.getTitle());
        resp.setCategory(expense.getCategory());
        resp.setAmount(expense.getAmount());
        resp.setPaidById(currentUser.getId());
        resp.setPaidByName(currentUser.getDisplayName());
        resp.setCreatedAt(expense.getCreatedAt());
        resp.setSplitMethod("Equal Split");
        resp.setSharePerPerson(shareAmount);
        return resp;
    }

    @Transactional
    public ExpenseResponse updateExpense(Long expenseId, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new BusinessException("Expense not found."));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            expense.setTitle(request.getTitle().trim());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            expense.setCategory(request.getCategory());
        }
        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            expense.setAmount(request.getAmount());
        }
        expense.setUpdatedAt(java.time.LocalDateTime.now());
        expense = expenseRepository.save(expense);

        // Update shares
        expenseShareRepository.deleteByExpense(expense);
        List<HouseholdMember> members = householdMemberRepository.findByHousehold(expense.getHousehold());
        int count = Math.max(members.size(), 1);
        BigDecimal shareAmount = expense.getAmount().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        for (HouseholdMember m : members) {
            ExpenseShare share = new ExpenseShare(expense, m.getUser(), shareAmount);
            expenseShareRepository.save(share);
        }

        ExpenseResponse resp = new ExpenseResponse();
        resp.setId(expense.getId());
        resp.setTitle(expense.getTitle());
        resp.setCategory(expense.getCategory());
        resp.setAmount(expense.getAmount());
        resp.setPaidById(expense.getPaidBy() != null ? expense.getPaidBy().getId() : null);
        resp.setPaidByName(expense.getPaidBy() != null ? expense.getPaidBy().getDisplayName() : "Unknown");
        resp.setCreatedAt(expense.getCreatedAt());
        resp.setSplitMethod("Equal Split");
        resp.setSharePerPerson(shareAmount);
        return resp;
    }

    @Transactional
    public void deleteExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new BusinessException("Expense not found."));
        expenseShareRepository.deleteByExpense(expense);
        expenseRepository.delete(expense);
    }
}
