package com.choresync.repository;

import com.choresync.entity.Expense;
import com.choresync.entity.Household;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByHouseholdOrderByCreatedAtDesc(Household household);
    List<Expense> findByHouseholdIdOrderByCreatedAtDesc(Long householdId);
    List<Expense> findByHousehold(Household household);
}
