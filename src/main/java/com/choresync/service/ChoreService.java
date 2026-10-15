package com.choresync.service;

import com.choresync.dto.ChoreDtos.ChoreRequest;
import com.choresync.dto.ChoreDtos.ChoreResponse;
import com.choresync.entity.Chore;
import com.choresync.entity.ChoreCompletion;
import com.choresync.entity.Household;
import com.choresync.entity.User;
import com.choresync.exception.BusinessException;
import com.choresync.repository.ChoreCompletionRepository;
import com.choresync.repository.ChoreRepository;
import com.choresync.repository.HouseholdRepository;
import com.choresync.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChoreService {

    private final ChoreRepository choreRepository;
    private final ChoreCompletionRepository choreCompletionRepository;
    private final HouseholdRepository householdRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public ChoreService(
        ChoreRepository choreRepository,
        ChoreCompletionRepository choreCompletionRepository,
        HouseholdRepository householdRepository,
        UserRepository userRepository,
        AuthService authService
    ) {
        this.choreRepository = choreRepository;
        this.choreCompletionRepository = choreCompletionRepository;
        this.householdRepository = householdRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<ChoreResponse> getChoresForHousehold(Long householdId) {
        Household household = householdRepository.findById(householdId)
            .orElseThrow(() -> new BusinessException("Household not found."));

        List<Chore> chores = choreRepository.findByHouseholdOrderByCreatedAtDesc(household);
        List<ChoreResponse> responses = new ArrayList<>();

        for (Chore chore : chores) {
            responses.add(mapToResponse(chore));
        }
        return responses;
    }

    @Transactional
    public ChoreResponse createChore(Long householdId, ChoreRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("Chore title is required.");
        }

        Household household = householdRepository.findById(householdId)
            .orElseThrow(() -> new BusinessException("Household not found."));

        User assignedTo = null;
        if (request.getAssignedToId() != null) {
            assignedTo = userRepository.findById(request.getAssignedToId()).orElse(null);
        }
        if (assignedTo == null) {
            assignedTo = authService.getCurrentUser();
        }

        BigDecimal points = request.getPoints() != null ? request.getPoints() : BigDecimal.valueOf(10);
        String difficulty = request.getDifficulty() != null && !request.getDifficulty().isBlank() ? request.getDifficulty() : "Medium";
        String description = request.getDescription() != null ? request.getDescription() : "";
        LocalDateTime dueDate = request.getDueDate() != null ? request.getDueDate() : LocalDateTime.now().plusDays(1);

        Chore chore = new Chore(
            household,
            assignedTo,
            request.getTitle().trim(),
            description,
            points,
            difficulty,
            request.isRecurring(),
            dueDate
        );

        chore = choreRepository.save(chore);
        return mapToResponse(chore);
    }

    @Transactional
    public ChoreResponse updateChore(Long choreId, ChoreRequest request) {
        Chore chore = choreRepository.findById(choreId)
            .orElseThrow(() -> new BusinessException("Chore not found."));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            chore.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            chore.setDescription(request.getDescription());
        }
        if (request.getPoints() != null) {
            chore.setPoints(request.getPoints());
        }
        if (request.getDifficulty() != null && !request.getDifficulty().isBlank()) {
            chore.setDifficulty(request.getDifficulty());
        }
        chore.setRecurring(request.isRecurring());
        if (request.getDueDate() != null) {
            chore.setDueDate(request.getDueDate());
        }
        if (request.getAssignedToId() != null) {
            User assignedTo = userRepository.findById(request.getAssignedToId()).orElse(null);
            chore.setAssignedTo(assignedTo);
        }
        chore.setUpdatedAt(LocalDateTime.now());

        chore = choreRepository.save(chore);
        return mapToResponse(chore);
    }

    @Transactional
    public void deleteChore(Long choreId) {
        Chore chore = choreRepository.findById(choreId)
            .orElseThrow(() -> new BusinessException("Chore not found."));
        choreCompletionRepository.deleteByChore(chore);
        choreRepository.delete(chore);
    }

    @Transactional
    public ChoreResponse completeChore(Long choreId, String notes) {
        User user = authService.getCurrentUserOrThrow();
        Chore chore = choreRepository.findById(choreId)
            .orElseThrow(() -> new BusinessException("Chore not found."));

        Optional<ChoreCompletion> existing = choreCompletionRepository.findTopByChoreOrderByCompletedAtDesc(chore);
        if (existing.isEmpty()) {
            ChoreCompletion completion = new ChoreCompletion(chore, user, true, notes != null ? notes : "Completed");
            choreCompletionRepository.save(completion);
        }

        return mapToResponse(chore);
    }

    @Transactional
    public ChoreResponse reopenChore(Long choreId) {
        Chore chore = choreRepository.findById(choreId)
            .orElseThrow(() -> new BusinessException("Chore not found."));

        choreCompletionRepository.deleteByChore(chore);
        return mapToResponse(chore);
    }

    private ChoreResponse mapToResponse(Chore chore) {
        ChoreResponse response = new ChoreResponse();
        response.setId(chore.getId());
        response.setTitle(chore.getTitle());
        response.setDescription(chore.getDescription());
        response.setPoints(chore.getPoints());
        response.setDifficulty(chore.getDifficulty());
        response.setRecurring(chore.isRecurring());
        response.setDueDate(chore.getDueDate());

        if (chore.getAssignedTo() != null) {
            response.setAssignedToId(chore.getAssignedTo().getId());
            response.setAssignedToName(chore.getAssignedTo().getDisplayName());
        } else {
            response.setAssignedToName("Unassigned");
        }

        Optional<ChoreCompletion> completionOpt = choreCompletionRepository.findTopByChoreOrderByCompletedAtDesc(chore);
        boolean isCompleted = completionOpt.isPresent();
        response.setCompleted(isCompleted);

        if (isCompleted) {
            response.setStatus("Completed");
            response.setCompletedAt(completionOpt.get().getCompletedAt());
            response.setCompletedByName(completionOpt.get().getCompletedBy().getDisplayName());
        } else {
            LocalDateTime now = LocalDateTime.now();
            if (chore.getDueDate() != null && chore.getDueDate().isBefore(now)) {
                response.setStatus("Overdue");
            } else {
                response.setStatus("Pending");
            }
        }

        return response;
    }
}
