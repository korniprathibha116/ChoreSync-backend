package com.choresync.repository;

import com.choresync.entity.Chore;
import com.choresync.entity.Household;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChoreRepository extends JpaRepository<Chore, Long> {
    List<Chore> findByHouseholdOrderByCreatedAtDesc(Household household);
    List<Chore> findByHouseholdIdOrderByDueDateAsc(Long householdId);
    List<Chore> findByHousehold(Household household);
    List<Chore> findByHouseholdId(Long householdId);
    long countByHousehold(Household household);
}
