package com.sisenco.weeklyreport.repository;

import com.sisenco.weeklyreport.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByIsActiveTrue();
    Optional<Project> findByNameIgnoreCase(String name);
}
