package com.japstransfort.config;

import com.japstransfort.entity.User;
import com.japstransfort.entity.UserRole;
import com.japstransfort.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Startup seeder to initialize the default admin account.
 * Reads password from ADMIN_SEED_PASSWORD environment variable with no hardcoded fallback.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_SEED_PASSWORD:#{null}}")
    private String adminSeedPassword;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        List<User> existingAdmins = userRepository.findByRole(UserRole.admin);

        if (existingAdmins.isEmpty()) {
            if (adminSeedPassword != null && !adminSeedPassword.trim().isEmpty()) {
                User admin = new User();
                admin.setFullName("System Administrator");
                admin.setEmail("admin@japstransfort.com");
                admin.setPasswordHash(passwordEncoder.encode(adminSeedPassword.trim()));
                admin.setRole(UserRole.admin);
                admin.setIsActive(true);

                userRepository.save(admin);
                logger.info("==================================================================");
                logger.info("Admin account successfully seeded for: admin@japstransfort.com");
                logger.info("==================================================================");
            } else {
                logger.warn("==================================================================");
                logger.warn("No admin user found and ADMIN_SEED_PASSWORD is not set.");
                logger.warn("To automatically seed the admin account on startup, set:");
                logger.warn("  ADMIN_SEED_PASSWORD=<your_password>");
                logger.warn("==================================================================");
            }
        } else {
            logger.info("Admin user already exists in the database. Seeding skipped.");
        }
    }
}
