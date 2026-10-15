package com.choresync.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ChoreDtos {
    public static class ChoreRequest {
        private String title;
        private String description;
        private BigDecimal points;
        private String difficulty;
        private boolean recurring;
        private LocalDateTime dueDate;

        private Long assignedToId;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPoints() { return points; }
        public void setPoints(BigDecimal points) { this.points = points; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public boolean isRecurring() { return recurring; }
        public void setRecurring(boolean recurring) { this.recurring = recurring; }
        public LocalDateTime getDueDate() { return dueDate; }
        public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
        public Long getAssignedToId() { return assignedToId; }
        public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }
    }

    public static class ChoreResponse {
        private Long id;
        private String title;
        private String description;
        private BigDecimal points;
        private String difficulty;
        private boolean recurring;
        private LocalDateTime dueDate;
        private Long assignedToId;
        private String assignedToName;
        private String status;
        private boolean completed;
        private LocalDateTime completedAt;
        private String completedByName;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPoints() { return points; }
        public void setPoints(BigDecimal points) { this.points = points; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public boolean isRecurring() { return recurring; }
        public void setRecurring(boolean recurring) { this.recurring = recurring; }
        public LocalDateTime getDueDate() { return dueDate; }
        public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
        public Long getAssignedToId() { return assignedToId; }
        public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }
        public String getAssignedToName() { return assignedToName; }
        public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public String getCompletedByName() { return completedByName; }
        public void setCompletedByName(String completedByName) { this.completedByName = completedByName; }
    }

    public static class CompletionRequest {
        private String notes;
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
