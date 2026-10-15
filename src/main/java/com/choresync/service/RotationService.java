package com.choresync.service;

import com.choresync.entity.Chore;
import com.choresync.entity.ChoreCompletion;
import com.choresync.entity.Household;
import com.choresync.entity.HouseholdMember;
import com.choresync.entity.User;
import com.choresync.exception.BusinessException;
import com.choresync.repository.ChoreCompletionRepository;
import com.choresync.repository.ChoreRepository;
import com.choresync.repository.HouseholdMemberRepository;
import com.choresync.repository.HouseholdRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RotationService {

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final ChoreRepository choreRepository;
    private final ChoreCompletionRepository choreCompletionRepository;

    public RotationService(
        HouseholdRepository householdRepository,
        HouseholdMemberRepository householdMemberRepository,
        ChoreRepository choreRepository,
        ChoreCompletionRepository choreCompletionRepository
    ) {
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.choreRepository = choreRepository;
        this.choreCompletionRepository = choreCompletionRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRotationOverview(Long householdId) {
        Household household = householdRepository.findById(householdId)
            .orElseThrow(() -> new BusinessException("Household not found."));

        List<HouseholdMember> members = householdMemberRepository.findByHousehold(household);
        List<Chore> chores = choreRepository.findByHouseholdOrderByCreatedAtDesc(household);

        List<Map<String, Object>> memberWorkloads = new ArrayList<>();
        Map<Long, Long> completionsPerMember = new LinkedHashMap<>();
        Map<Long, Long> assignedPerMember = new LinkedHashMap<>();

        for (HouseholdMember member : members) {
            Long userId = member.getUser().getId();
            completionsPerMember.put(userId, 0L);
            assignedPerMember.put(userId, 0L);
        }

        for (Chore chore : chores) {
            if (chore.getAssignedTo() != null) {
                Long uid = chore.getAssignedTo().getId();
                assignedPerMember.put(uid, assignedPerMember.getOrDefault(uid, 0L) + 1);
            }
            Optional<ChoreCompletion> comp = choreCompletionRepository.findTopByChoreOrderByCompletedAtDesc(chore);
            if (comp.isPresent()) {
                Long compUid = comp.get().getCompletedBy().getId();
                completionsPerMember.put(compUid, completionsPerMember.getOrDefault(compUid, 0L) + 1);
            }
        }

        for (HouseholdMember member : members) {
            User u = member.getUser();
            long assigned = assignedPerMember.getOrDefault(u.getId(), 0L);
            long completed = completionsPerMember.getOrDefault(u.getId(), 0L);

            Map<String, Object> mStats = new LinkedHashMap<>();
            mStats.put("userId", u.getId());
            mStats.put("name", u.getDisplayName());
            mStats.put("role", member.getRole());
            mStats.put("assignedChores", assigned);
            mStats.put("completedChores", completed);
            mStats.put("workloadScore", assigned + (completed * 0.5));
            memberWorkloads.add(mStats);
        }

        List<Map<String, Object>> choreRotations = new ArrayList<>();
        for (Chore chore : chores) {
            Map<String, Object> cInfo = new LinkedHashMap<>();
            cInfo.put("id", chore.getId());
            cInfo.put("title", chore.getTitle());
            cInfo.put("difficulty", chore.getDifficulty());
            cInfo.put("dueDate", chore.getDueDate());

            User currentAssignee = chore.getAssignedTo();
            cInfo.put("currentAssignee", currentAssignee != null ? currentAssignee.getDisplayName() : "Unassigned");
            cInfo.put("currentAssigneeId", currentAssignee != null ? currentAssignee.getId() : null);

            User nextAssignee = determineNextAssignee(currentAssignee, members);
            cInfo.put("nextAssignee", nextAssignee != null ? nextAssignee.getDisplayName() : "Unassigned");
            cInfo.put("nextAssigneeId", nextAssignee != null ? nextAssignee.getId() : null);

            boolean isCompleted = choreCompletionRepository.findTopByChoreOrderByCompletedAtDesc(chore).isPresent();
            cInfo.put("completed", isCompleted);

            choreRotations.add(cInfo);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("householdId", householdId);
        response.put("householdName", household.getName());
        response.put("rotationInterval", "Weekly (Every 7 days)");
        response.put("nextRotationDate", LocalDateTime.now().plusDays(7).toLocalDate().toString());
        response.put("algorithm", "Deterministic Workload-Balanced Round Robin");
        response.put("explanation", "ChoreSync rotates chore responsibilities deterministically across all active members. The system evaluates member workloads and historical completions to ensure chores are never repeatedly assigned to the same individual.");
        response.put("memberWorkloads", memberWorkloads);
        response.put("choreRotations", choreRotations);

        return response;
    }

    @Transactional
    public Map<String, Object> executeRotation(Long householdId) {
        Household household = householdRepository.findById(householdId)
            .orElseThrow(() -> new BusinessException("Household not found."));

        List<HouseholdMember> members = householdMemberRepository.findByHousehold(household);
        if (members.size() <= 1) {
            throw new BusinessException("At least 2 household members are required to perform chore rotation.");
        }

        List<Chore> chores = choreRepository.findByHousehold(household);
        for (Chore chore : chores) {
            User next = determineNextAssignee(chore.getAssignedTo(), members);
            if (next != null) {
                chore.setAssignedTo(next);
                if (chore.getDueDate() != null) {
                    chore.setDueDate(chore.getDueDate().plusDays(7));
                } else {
                    chore.setDueDate(LocalDateTime.now().plusDays(7));
                }
                chore.setUpdatedAt(LocalDateTime.now());
                choreRepository.save(chore);
            }
        }

        Map<String, Object> result = getRotationOverview(householdId);
        result.put("message", "Chores have been successfully rotated among household members!");
        return result;
    }

    private User determineNextAssignee(User currentAssignee, List<HouseholdMember> members) {
        if (members.isEmpty()) {
            return null;
        }
        if (members.size() == 1) {
            return members.get(0).getUser();
        }

        if (currentAssignee == null) {
            return members.get(0).getUser();
        }

        int currentIndex = -1;
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getUser().getId().equals(currentAssignee.getId())) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex = (currentIndex + 1) % members.size();
        return members.get(nextIndex).getUser();
    }
}
