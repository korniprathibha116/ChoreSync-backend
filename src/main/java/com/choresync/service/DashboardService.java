package com.choresync.service;

import com.choresync.entity.Chore;
import com.choresync.entity.ChoreCompletion;
import com.choresync.entity.Expense;
import com.choresync.entity.Household;
import com.choresync.entity.HouseholdMember;
import com.choresync.entity.User;
import com.choresync.exception.BusinessException;
import com.choresync.repository.ChoreCompletionRepository;
import com.choresync.repository.ChoreRepository;
import com.choresync.repository.ExpenseRepository;
import com.choresync.repository.HouseholdMemberRepository;
import com.choresync.repository.HouseholdRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final ChoreRepository choreRepository;
    private final ChoreCompletionRepository choreCompletionRepository;
    private final ExpenseRepository expenseRepository;
    private final AuthService authService;

    public DashboardService(
        HouseholdRepository householdRepository,
        HouseholdMemberRepository householdMemberRepository,
        ChoreRepository choreRepository,
        ChoreCompletionRepository choreCompletionRepository,
        ExpenseRepository expenseRepository,
        AuthService authService
    ) {
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.choreRepository = choreRepository;
        this.choreCompletionRepository = choreCompletionRepository;
        this.expenseRepository = expenseRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(Long householdId) {
        User currentUser = authService.getCurrentUserOrThrow();
        Household household = householdRepository.findById(householdId)
            .orElseThrow(() -> new BusinessException("Household not found."));

        List<HouseholdMember> members = householdMemberRepository.findByHousehold(household);
        List<Chore> chores = choreRepository.findByHouseholdOrderByCreatedAtDesc(household);
        List<Expense> expenses = expenseRepository.findByHouseholdOrderByCreatedAtDesc(household);

        int totalChores = chores.size();
        int completedChores = 0;
        int pendingChores = 0;
        int overdueChores = 0;
        LocalDateTime now = LocalDateTime.now();

        List<Map<String, Object>> upcomingChores = new ArrayList<>();
        List<Map<String, Object>> recentActivity = new ArrayList<>();

        for (Chore chore : chores) {
            Optional<ChoreCompletion> compOpt = choreCompletionRepository.findTopByChoreOrderByCompletedAtDesc(chore);
            boolean done = compOpt.isPresent();

            if (done) {
                completedChores++;
                ChoreCompletion comp = compOpt.get();
                Map<String, Object> act = new LinkedHashMap<>();
                act.put("id", "c-" + comp.getId());
                act.put("type", "chore_completion");
                act.put("user", comp.getCompletedBy().getDisplayName());
                act.put("isMe", comp.getCompletedBy().getId().equals(currentUser.getId()));
                act.put("text", (comp.getCompletedBy().getId().equals(currentUser.getId()) ? "You" : comp.getCompletedBy().getDisplayName()) + " completed " + chore.getTitle());
                act.put("time", "Recently");
                act.put("timestamp", comp.getCompletedAt());
                recentActivity.add(act);
            } else {
                pendingChores++;
                boolean isOverdue = chore.getDueDate() != null && chore.getDueDate().isBefore(now);
                if (isOverdue) {
                    overdueChores++;
                }

                // Collect pending chore for upcoming list
                if (upcomingChores.size() < 5) {
                    Map<String, Object> u = new LinkedHashMap<>();
                    u.put("id", chore.getId());
                    u.put("title", chore.getTitle());
                    u.put("assignedTo", chore.getAssignedTo() != null ? chore.getAssignedTo().getDisplayName() : "Unassigned");
                    u.put("isAssignedToMe", chore.getAssignedTo() != null && chore.getAssignedTo().getId().equals(currentUser.getId()));
                    u.put("priority", chore.getDifficulty() != null ? chore.getDifficulty() : "Medium");
                    u.put("dueDate", chore.getDueDate());
                    u.put("isOverdue", isOverdue);
                    upcomingChores.add(u);
                }
            }
        }

        // Add recent expenses to activity
        for (Expense exp : expenses) {
            if (recentActivity.size() >= 8) break;
            Map<String, Object> act = new LinkedHashMap<>();
            act.put("id", "e-" + exp.getId());
            act.put("type", "expense_created");
            act.put("user", exp.getPaidBy() != null ? exp.getPaidBy().getDisplayName() : "Someone");
            act.put("isMe", exp.getPaidBy() != null && exp.getPaidBy().getId().equals(currentUser.getId()));
            act.put("text", (exp.getPaidBy() != null && exp.getPaidBy().getId().equals(currentUser.getId()) ? "You" : exp.getPaidBy().getDisplayName()) + " added a new expense: " + exp.getTitle());
            act.put("time", "Recently");
            act.put("timestamp", exp.getCreatedAt());
            recentActivity.add(act);
        }

        // Calculate total expenses and user balance
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal userPaid = BigDecimal.ZERO;
        int memberCount = Math.max(members.size(), 1);

        for (Expense exp : expenses) {
            BigDecimal amt = exp.getAmount() != null ? exp.getAmount() : BigDecimal.ZERO;
            totalExpenses = totalExpenses.add(amt);
            if (exp.getPaidBy() != null && exp.getPaidBy().getId().equals(currentUser.getId())) {
                userPaid = userPaid.add(amt);
            }
        }

        BigDecimal userFairShare = totalExpenses.divide(BigDecimal.valueOf(memberCount), 2, RoundingMode.HALF_UP);
        BigDecimal userBalance = userPaid.subtract(userFairShare);

        int completionRate = totalChores > 0 ? (int) Math.round(((double) completedChores / totalChores) * 100) : 0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("householdId", household.getId());
        response.put("householdName", household.getName());
        response.put("householdMembers", members.size());
        response.put("totalChores", totalChores);
        response.put("pendingChores", pendingChores);
        response.put("completedChores", completedChores);
        response.put("overdueChores", overdueChores);
        response.put("totalExpenses", totalExpenses);
        response.put("userBalance", userBalance);
        response.put("completionRate", completionRate);
        response.put("upcomingChores", upcomingChores);
        response.put("recentActivity", recentActivity);

        return response;
    }
}
