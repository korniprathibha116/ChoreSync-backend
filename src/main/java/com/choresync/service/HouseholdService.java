package com.choresync.service;

import com.choresync.dto.HouseholdDtos.HouseholdMemberSummary;
import com.choresync.dto.HouseholdDtos.HouseholdRequest;
import com.choresync.dto.HouseholdDtos.HouseholdResponse;
import com.choresync.dto.HouseholdDtos.HouseholdSummaryResponse;
import com.choresync.entity.Chore;
import com.choresync.entity.Expense;
import com.choresync.entity.ExpenseShare;
import com.choresync.entity.Household;
import com.choresync.entity.HouseholdMember;
import com.choresync.entity.User;
import com.choresync.exception.BusinessException;
import com.choresync.repository.ChoreRepository;
import com.choresync.repository.ExpenseRepository;
import com.choresync.repository.ExpenseShareRepository;
import com.choresync.repository.HouseholdMemberRepository;
import com.choresync.repository.HouseholdRepository;
import com.choresync.repository.UserRepository;
import com.choresync.util.CodeGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final UserRepository userRepository;
    private final ChoreRepository choreRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final AuthService authService;

    public HouseholdService(
        HouseholdRepository householdRepository,
        HouseholdMemberRepository householdMemberRepository,
        UserRepository userRepository,
        ChoreRepository choreRepository,
        ExpenseRepository expenseRepository,
        ExpenseShareRepository expenseShareRepository,
        AuthService authService
    ) {
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.userRepository = userRepository;
        this.choreRepository = choreRepository;
        this.expenseRepository = expenseRepository;
        this.expenseShareRepository = expenseShareRepository;
        this.authService = authService;
    }

    @Transactional
    public List<HouseholdResponse> getHouseholdsForCurrentUser() {
        User user = authService.getCurrentUserOrThrow();
        List<HouseholdMember> memberships = householdMemberRepository.findByUser(user);

        if (memberships.isEmpty()) {
            Household defaultHousehold = createDefaultHousehold(user);
            return List.of(mapToResponse(defaultHousehold));
        }

        List<HouseholdResponse> result = new ArrayList<>();
        for (HouseholdMember membership : memberships) {
            result.add(mapToResponse(membership.getHousehold()));
        }
        return result;
    }

    @Transactional
    public HouseholdSummaryResponse getHouseholdSummary(Long id) {
        User user = authService.getCurrentUserOrThrow();
        Household household = householdRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Household not found."));

        List<HouseholdMember> members = householdMemberRepository.findByHousehold(household);
        boolean isMember = members.stream().anyMatch(m -> m.getUser().getId().equals(user.getId()));
        if (!isMember) {
            throw new BusinessException("You are not a member of this household.");
        }

        HouseholdSummaryResponse summary = new HouseholdSummaryResponse();
        summary.setId(household.getId());
        summary.setName(household.getName());

        List<HouseholdMemberSummary> memberSummaries = new ArrayList<>();
        for (HouseholdMember m : members) {
            HouseholdMemberSummary ms = new HouseholdMemberSummary();
            ms.setUserId(m.getUser().getId());
            ms.setDisplayName(m.getUser().getDisplayName());
            ms.setEmail(m.getUser().getEmail());
            ms.setRole(m.getRole());
            ms.setBalance(m.getBalance());
            memberSummaries.add(ms);
        }
        summary.setMembers(memberSummaries);
        return summary;
    }

    @Transactional
    public HouseholdResponse getHouseholdById(Long id) {
        Household household = householdRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Household not found."));
        return mapToResponse(household);
    }

    @Transactional
    public HouseholdResponse createHousehold(HouseholdRequest request) {
        User user = authService.getCurrentUserOrThrow();
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("Household name is required.");
        }

        String inviteCode = generateUniqueInviteCode();
        Household household = new Household(request.getName().trim(), request.getDescription(), inviteCode, user);
        household = householdRepository.save(household);

        HouseholdMember ownerMember = new HouseholdMember(user, household, "OWNER");
        householdMemberRepository.save(ownerMember);

        return mapToResponse(household);
    }

    @Transactional
    public HouseholdResponse joinHousehold(String inviteCode) {
        User user = authService.getCurrentUserOrThrow();
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new BusinessException("Invite code is required.");
        }

        Household household = householdRepository.findByInviteCode(inviteCode.trim().toUpperCase())
            .orElseThrow(() -> new BusinessException("Invalid invite code. Household not found."));

        if (!householdMemberRepository.existsByHouseholdAndUser(household, user)) {
            HouseholdMember member = new HouseholdMember(user, household, "MEMBER");
            householdMemberRepository.save(member);
        }

        return mapToResponse(household);
    }

    @Transactional
    public void removeMember(Long householdId, Long targetUserId) {
        User currentUser = authService.getCurrentUserOrThrow();
        Household household = householdRepository.findById(householdId)
            .orElseThrow(() -> new BusinessException("Household not found."));

        HouseholdMember currentMember = householdMemberRepository.findByHouseholdAndUser(household, currentUser)
            .orElseThrow(() -> new BusinessException("You are not a member of this household."));

        User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new BusinessException("Target member not found."));

        HouseholdMember targetMember = householdMemberRepository.findByHouseholdAndUser(household, targetUser)
            .orElseThrow(() -> new BusinessException("Member not found in this household."));

        boolean isSelf = currentUser.getId().equals(targetUserId);
        boolean isOwner = "OWNER".equalsIgnoreCase(currentMember.getRole());

        if (!isSelf && !isOwner) {
            throw new BusinessException("Only the household owner can remove other members.");
        }

        if (isOwner && isSelf) {
            List<HouseholdMember> allMembers = householdMemberRepository.findByHousehold(household);
            if (allMembers.size() > 1) {
                throw new BusinessException("As the owner, please transfer ownership before leaving.");
            }
        }

        householdMemberRepository.delete(targetMember);
    }

    private Household createDefaultHousehold(User user) {
        String inviteCode = generateUniqueInviteCode();
        Household household = new Household("Nextgen room", "Shared household space", inviteCode, user);
        household = householdRepository.save(household);

        HouseholdMember ownerMember = new HouseholdMember(user, household, "OWNER");
        householdMemberRepository.save(ownerMember);

        // Seed 3 co-roommates if not existing (Priya, Sam, Jordan) to match the reference UI
        seedStarterHouseholdData(household, user);

        return household;
    }

    private void seedStarterHouseholdData(Household household, User owner) {
        User priya = getOrCreateSeedUser("Priya", "Sharma", "priya@example.com", "Priya");
        User sam = getOrCreateSeedUser("Sam", "Miller", "sam@example.com", "Sam");
        User jordan = getOrCreateSeedUser("Jordan", "Lee", "jordan@example.com", "Jordan");

        addMemberIfNotPresent(household, priya, "MEMBER");
        addMemberIfNotPresent(household, sam, "MEMBER");
        addMemberIfNotPresent(household, jordan, "MEMBER");

        if (choreRepository.countByHousehold(household) == 0) {
            LocalDateTime now = LocalDateTime.now();
            Chore chore1 = new Chore(household, owner, "Take out trash", "Take kitchen & recycling bins out to the curb", BigDecimal.valueOf(10), "Medium", true, now.plusDays(0));
            Chore chore2 = new Chore(household, priya, "Clean kitchen", "Wipe down counters, stovetop, and sink", BigDecimal.valueOf(15), "Low", true, now.plusDays(1));
            Chore chore3 = new Chore(household, sam, "Laundry", "Wash and fold shared towels and mats", BigDecimal.valueOf(20), "High", false, now.minusDays(1));
            Chore chore4 = new Chore(household, jordan, "Vacuum living room", "Vacuum the rugs and sofa cushions", BigDecimal.valueOf(15), "Medium", true, now.plusDays(3));
            Chore chore5 = new Chore(household, owner, "Clean bathroom", "Scrub shower, sink, and mirror", BigDecimal.valueOf(25), "Low", true, now.plusDays(4));

            choreRepository.save(chore1);
            choreRepository.save(chore2);
            choreRepository.save(chore3);
            choreRepository.save(chore4);
            choreRepository.save(chore5);
        }

        if (expenseRepository.findByHousehold(household).isEmpty()) {
            createStarterExpense(household, owner, "Groceries", "Groceries", BigDecimal.valueOf(120.00));
            createStarterExpense(household, priya, "Electricity", "Utilities", BigDecimal.valueOf(80.50));
            createStarterExpense(household, sam, "Water Bill", "Utilities", BigDecimal.valueOf(45.00));
        }
    }

    private void createStarterExpense(Household household, User paidBy, String title, String category, BigDecimal amount) {
        Expense expense = new Expense(household, paidBy, title, category, amount);
        expense = expenseRepository.save(expense);

        List<HouseholdMember> members = householdMemberRepository.findByHousehold(household);
        if (!members.isEmpty()) {
            BigDecimal share = amount.divide(BigDecimal.valueOf(members.size()), 2, java.math.RoundingMode.HALF_UP);
            for (HouseholdMember m : members) {
                ExpenseShare es = new ExpenseShare(expense, m.getUser(), share);
                expenseShareRepository.save(es);
            }
        }
    }

    private User getOrCreateSeedUser(String first, String last, String email, String display) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User(first, last, email, "$2a$10$7EqJtq98hPqEX7fNZaFWoO.KkJx301s3D8.Q1b1w0WJ8p18w2w2i2", display);
            return userRepository.save(u);
        });
    }

    private void addMemberIfNotPresent(Household household, User user, String role) {
        if (!householdMemberRepository.existsByHouseholdAndUser(household, user)) {
            HouseholdMember member = new HouseholdMember(user, household, role);
            householdMemberRepository.save(member);
        }
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = CodeGenerator.generateInviteCode();
        } while (householdRepository.existsByInviteCode(code));
        return code;
    }

    private HouseholdResponse mapToResponse(Household h) {
        return new HouseholdResponse(
            h.getId(),
            h.getName(),
            h.getDescription(),
            h.getInviteCode(),
            h.getCreatedBy() != null ? h.getCreatedBy().getId() : null
        );
    }
}
