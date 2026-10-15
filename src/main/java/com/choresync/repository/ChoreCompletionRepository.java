package com.choresync.repository;

import com.choresync.entity.Chore;
import com.choresync.entity.ChoreCompletion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChoreCompletionRepository extends JpaRepository<ChoreCompletion, Long> {
    List<ChoreCompletion> findByChore(Chore chore);
    long countByChoreId(Long choreId);
    java.util.Optional<ChoreCompletion> findTopByChoreOrderByCompletedAtDesc(Chore chore);
    void deleteByChore(Chore chore);
}
