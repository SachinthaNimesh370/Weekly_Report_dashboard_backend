package com.sisenco.weeklyreport.util;

import com.sisenco.weeklyreport.entity.Role;
import com.sisenco.weeklyreport.entity.User;
import com.sisenco.weeklyreport.entity.enums.RoleName;
import com.sisenco.weeklyreport.repository.RoleRepository;
import com.sisenco.weeklyreport.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking and seeding default roles and seed users...");

        Role memberRole = roleRepository.findByName(RoleName.ROLE_TEAM_MEMBER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_TEAM_MEMBER).build()));

        Role managerRole = roleRepository.findByName(RoleName.ROLE_MANAGER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_MANAGER).build()));

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_ADMIN).build()));

        // Seed default Admin if not exists
        if (!userRepository.existsByEmail("admin@weeklyreport.com")) {
            User admin = User.builder()
                    .fullName("System Administrator")
                    .email("admin@weeklyreport.com")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .role(adminRole)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            log.info("Default admin user created: admin@weeklyreport.com / Admin@123");
        }

        // Seed default Manager if not exists
        if (!userRepository.existsByEmail("manager@weeklyreport.com")) {
            User manager = User.builder()
                    .fullName("Lead Manager")
                    .email("manager@weeklyreport.com")
                    .passwordHash(passwordEncoder.encode("Manager@123"))
                    .role(managerRole)
                    .isActive(true)
                    .build();
            userRepository.save(manager);
            log.info("Default manager user created: manager@weeklyreport.com / Manager@123");
        }

        // Seed default Team Member if not exists
        if (!userRepository.existsByEmail("member@weeklyreport.com")) {
            User member = User.builder()
                    .fullName("John Developer")
                    .email("member@weeklyreport.com")
                    .passwordHash(passwordEncoder.encode("Member@123"))
                    .role(memberRole)
                    .isActive(true)
                    .build();
            userRepository.save(member);
            log.info("Default team member user created: member@weeklyreport.com / Member@123");
        }
    }
}
