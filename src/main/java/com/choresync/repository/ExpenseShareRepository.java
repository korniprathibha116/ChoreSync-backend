package com.choresync.repository;

import com.choresync.entity.Expense;
import com.choresync.entity.ExpenseShare;
import com.choresync.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {
    List<ExpenseShare> findByExpense(Expense expense);
    Optional<ExpenseShare> findByExpenseAndUser(Expense expense, User user);
    List<ExpenseShare> findByUser(User user);
    void deleteByExpense(Expense expense);
}
