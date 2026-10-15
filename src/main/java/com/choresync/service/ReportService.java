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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final ChoreRepository choreRepository;
    private final ChoreCompletionRepository choreCompletionRepository;
    private final ExpenseRepository expenseRepository;

    public ReportService(
        HouseholdRepository householdRepository,
        HouseholdMemberRepository householdMemberRepository,
        ChoreRepository choreRepository,
        ChoreCompletionRepository choreCompletionRepository,
        ExpenseRepository expenseRepository
    ) {
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.choreRepository = choreRepository;
        this.choreCompletionRepository = choreCompletionRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReports(Long householdId, String timeRange) {
        Household household = householdRepository.findById(householdId)
            .orElseThrow(() -> new BusinessException("Household not found."));

        List<HouseholdMember> members = householdMemberRepository.findByHousehold(household);
        List<Chore> chores = choreRepository.findByHousehold(household);
        List<Expense> expenses = expenseRepository.findByHousehold(household);

        // 1. Chores by Member (bar chart data)
        List<Map<String, Object>> choresByMember = new ArrayList<>();
        int totalCompletedOverall = 0;
        int totalPendingOverall = 0;

        for (HouseholdMember m : members) {
            User u = m.getUser();
            int completedCount = 0;
            int pendingCount = 0;

            for (Chore c : chores) {
                Optional<ChoreCompletion> comp = choreCompletionRepository.findTopByChoreOrderByCompletedAtDesc(c);
                if (comp.isPresent()) {
                    if (comp.get().getCompletedBy().getId().equals(u.getId())) {
                        completedCount++;
                    }
                } else if (c.getAssignedTo() != null && c.getAssignedTo().getId().equals(u.getId())) {
                    pendingCount++;
                }
            }

            totalCompletedOverall += completedCount;
            totalPendingOverall += pendingCount;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", u.getDisplayName());
            entry.put("userId", u.getId());
            entry.put("completed", completedCount);
            entry.put("pending", pendingCount);
            choresByMember.add(entry);
        }

        // 2. Expense Breakdown by Category (pie/donut chart)
        Map<String, BigDecimal> categorySums = new LinkedHashMap<>();
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Expense exp : expenses) {
            String cat = exp.getCategory() != null && !exp.getCategory().isBlank() ? exp.getCategory() : "Other";
            BigDecimal amt = exp.getAmount() != null ? exp.getAmount() : BigDecimal.ZERO;
            categorySums.put(cat, categorySums.getOrDefault(cat, BigDecimal.ZERO).add(amt));
            totalExpenses = totalExpenses.add(amt);
        }

        List<Map<String, Object>> expenseCategories = new ArrayList<>();
        String[] pastelColors = new String[] { "#9B7AE8", "#4ECDC4", "#FFD166", "#FF9F9B", "#A8DADC", "#BDB2FF" };
        int colorIdx = 0;

        for (Map.Entry<String, BigDecimal> entry : categorySums.entrySet()) {
            Map<String, Object> catMap = new LinkedHashMap<>();
            catMap.put("name", entry.getKey());
            catMap.put("value", entry.getValue());
            double pct = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                ? entry.getValue().divide(totalExpenses, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;
            catMap.put("percentage", Math.round(pct * 10.0) / 10.0);
            catMap.put("color", pastelColors[colorIdx % pastelColors.length]);
            colorIdx++;
            expenseCategories.add(catMap);
        }

        // 3. Workload distribution
        List<Map<String, Object>> workloadDistribution = new ArrayList<>();
        int totalChores = chores.size();
        for (HouseholdMember m : members) {
            User u = m.getUser();
            long assigned = chores.stream().filter(c -> c.getAssignedTo() != null && c.getAssignedTo().getId().equals(u.getId())).count();
            double pct = totalChores > 0 ? ((double) assigned / totalChores) * 100 : 0.0;
            Map<String, Object> wl = new LinkedHashMap<>();
            wl.put("name", u.getDisplayName());
            wl.put("choresCount", assigned);
            wl.put("percentage", Math.round(pct * 10.0) / 10.0);
            workloadDistribution.add(wl);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("householdId", householdId);
        result.put("timeRange", timeRange != null ? timeRange : "Last 30 Days");
        result.put("totalChores", totalChores);
        result.put("totalCompleted", totalCompletedOverall);
        result.put("totalPending", totalPendingOverall);
        result.put("totalExpenses", totalExpenses);
        result.put("choresByMember", choresByMember);
        result.put("expenseBreakdown", expenseCategories);
        result.put("workloadDistribution", workloadDistribution);

        return result;
    }
}
