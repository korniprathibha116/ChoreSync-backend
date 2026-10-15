package com.choresync.repository;

import com.choresync.entity.Household;
import com.choresync.entity.HouseholdMember;
import com.choresync.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, Long> {
    List<HouseholdMember> findByHousehold(Household household);
    Optional<HouseholdMember> findByHouseholdAndUser(Household household, User user);
    boolean existsByHouseholdAndUser(Household household, User user);
    List<HouseholdMember> findByUser(User user);
}
