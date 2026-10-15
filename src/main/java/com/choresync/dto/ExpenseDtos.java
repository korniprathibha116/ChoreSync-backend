package com.choresync.dto;

import java.math.BigDecimal;

public class ExpenseDtos {
    public static class ExpenseRequest {
        private String title;
        private String category;
        private BigDecimal amount;
        private String splitMethod;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getSplitMethod() { return splitMethod; }
        public void setSplitMethod(String splitMethod) { this.splitMethod = splitMethod; }
    }

    public static class ExpenseResponse {
        private Long id;
        private String title;
        private String category;
        private BigDecimal amount;
        private Long paidById;
        private String paidByName;
        private java.time.LocalDateTime createdAt;
        private String splitMethod;
        private BigDecimal sharePerPerson;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public Long getPaidById() { return paidById; }
        public void setPaidById(Long paidById) { this.paidById = paidById; }
        public String getPaidByName() { return paidByName; }
        public void setPaidByName(String paidByName) { this.paidByName = paidByName; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        public String getSplitMethod() { return splitMethod; }
        public void setSplitMethod(String splitMethod) { this.splitMethod = splitMethod; }
        public BigDecimal getSharePerPerson() { return sharePerPerson; }
        public void setSharePerPerson(BigDecimal sharePerPerson) { this.sharePerPerson = sharePerPerson; }
    }

    public static class ExpenseSummaryResponse {
        private BigDecimal totalExpenses;
        private BigDecimal userShare;
        private BigDecimal userBalance;
        private java.util.List<ExpenseResponse> expenses;

        public ExpenseSummaryResponse() {}

        public ExpenseSummaryResponse(BigDecimal totalExpenses, BigDecimal userShare, BigDecimal userBalance, java.util.List<ExpenseResponse> expenses) {
            this.totalExpenses = totalExpenses;
            this.userShare = userShare;
            this.userBalance = userBalance;
            this.expenses = expenses;
        }

        public BigDecimal getTotalExpenses() { return totalExpenses; }
        public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }
        public BigDecimal getUserShare() { return userShare; }
        public void setUserShare(BigDecimal userShare) { this.userShare = userShare; }
        public BigDecimal getUserBalance() { return userBalance; }
        public void setUserBalance(BigDecimal userBalance) { this.userBalance = userBalance; }
        public java.util.List<ExpenseResponse> getExpenses() { return expenses; }
        public void setExpenses(java.util.List<ExpenseResponse> expenses) { this.expenses = expenses; }
    }
}
