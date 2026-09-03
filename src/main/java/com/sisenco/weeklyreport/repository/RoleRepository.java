package com.sisenco.weeklyreport.repository;

import com.sisenco.weeklyreport.entity.Role;
import com.sisenco.weeklyreport.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
