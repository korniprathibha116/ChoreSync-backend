package com.choresync.dto;

import java.util.List;

public class HouseholdDtos {
    public static class HouseholdRequest {
        private String name;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class HouseholdResponse {
        private Long id;
        private String name;
        private String description;
        private String inviteCode;
        private Long createdById;

        public HouseholdResponse() {}

        public HouseholdResponse(Long id, String name, String description, String inviteCode, Long createdById) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.inviteCode = inviteCode;
            this.createdById = createdById;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getInviteCode() { return inviteCode; }
        public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
        public Long getCreatedById() { return createdById; }
        public void setCreatedById(Long createdById) { this.createdById = createdById; }
    }

    public static class HouseholdMemberSummary {
        private Long userId;
        private String displayName;
        private String email;
        private String role;
        private java.math.BigDecimal balance;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public java.math.BigDecimal getBalance() { return balance; }
        public void setBalance(java.math.BigDecimal balance) { this.balance = balance; }
    }

    public static class HouseholdSummaryResponse {
        private Long id;
        private String name;
        private List<HouseholdMemberSummary> members;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<HouseholdMemberSummary> getMembers() { return members; }
        public void setMembers(List<HouseholdMemberSummary> members) { this.members = members; }
    }
}
