package com.japstransfort.service;

import com.japstransfort.dto.ResellerFormDto;
import com.japstransfort.entity.Reseller;
import com.japstransfort.entity.ResellerTier;
import com.japstransfort.entity.User;
import com.japstransfort.entity.UserRole;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.ResellerTierRepository;
import com.japstransfort.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Service encapsulating business logic for Reseller management.
 */
@Service
public class ResellerService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "resellerId", "businessName", "contactNo", "territory", "creditLimit", "balance", "status", "createdAt"
    );

    private final ResellerRepository resellerRepository;
    private final ResellerTierRepository tierRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ResellerService(ResellerRepository resellerRepository,
                           ResellerTierRepository tierRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.resellerRepository = resellerRepository;
        this.tierRepository = tierRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<Reseller> getResellers(String search, Integer tierId, String status,
                                       int page, int size, String sortField, String sortDir) {
        // Normalize status filter
        String normalizedStatus = null;
        if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status)) {
            normalizedStatus = status.trim().toLowerCase();
        }

        // Validate sort field to prevent invalid property injection
        String validSortField = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "businessName";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : 10, Sort.by(direction, validSortField));

        return resellerRepository.findWithFilters(
                (search != null && !search.trim().isEmpty()) ? search.trim() : null,
                tierId,
                normalizedStatus,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Reseller findById(Integer id) {
        return resellerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reseller not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Reseller> findAllActive(Integer excludeResellerId) {
        return resellerRepository.findAll().stream()
                .filter(r -> "active".equalsIgnoreCase(r.getStatus()))
                .filter(r -> excludeResellerId == null || !r.getResellerId().equals(excludeResellerId))
                .toList();
    }

    @Transactional
    public Reseller create(ResellerFormDto dto) {
        String trimmedEmail = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new IllegalArgumentException("A user with email '" + trimmedEmail + "' already exists.");
        }

        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required for the reseller login user.");
        }

        ResellerTier tier = tierRepository.findById(dto.getTierId())
                .orElseThrow(() -> new IllegalArgumentException("Selected tier does not exist."));

        Reseller parentReseller = null;
        if (dto.getParentResellerId() != null) {
            parentReseller = resellerRepository.findById(dto.getParentResellerId())
                    .orElse(null);
        }

        // Create and persist user account
        User user = new User();
        user.setFullName(dto.getFullName().trim());
        user.setEmail(trimmedEmail);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword().trim()));
        user.setRole(UserRole.reseller);
        user.setIsActive(true);
        User savedUser = userRepository.save(user);

        // Create and persist reseller profile
        Reseller reseller = new Reseller();
        reseller.setUser(savedUser);
        reseller.setBusinessName(dto.getBusinessName().trim());
        reseller.setContactNo(dto.getContactNo() != null ? dto.getContactNo().trim() : null);
        reseller.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        reseller.setTerritory(dto.getTerritory() != null ? dto.getTerritory().trim() : null);
        reseller.setTier(tier);
        reseller.setParentReseller(parentReseller);
        reseller.setCreditLimit(dto.getCreditLimit() != null ? dto.getCreditLimit() : BigDecimal.ZERO);
        reseller.setBalance(BigDecimal.ZERO);
        reseller.setStatus(dto.getStatus() != null && !dto.getStatus().isBlank() ? dto.getStatus().trim().toLowerCase() : "active");

        return resellerRepository.save(reseller);
    }

    @Transactional
    public Reseller update(Integer id, ResellerFormDto dto) {
        Reseller reseller = findById(id);

        User user = reseller.getUser();
        if (user != null) {
            String trimmedEmail = dto.getEmail().trim().toLowerCase();
            userRepository.findByEmail(trimmedEmail).ifPresent(existing -> {
                if (!existing.getUserId().equals(user.getUserId())) {
                    throw new IllegalArgumentException("Another user with email '" + trimmedEmail + "' already exists.");
                }
            });

            user.setFullName(dto.getFullName().trim());
            user.setEmail(trimmedEmail);
            if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                user.setPasswordHash(passwordEncoder.encode(dto.getPassword().trim()));
            }
            userRepository.save(user);
        }

        ResellerTier tier = tierRepository.findById(dto.getTierId())
                .orElseThrow(() -> new IllegalArgumentException("Selected tier does not exist."));

        Reseller parentReseller = null;
        if (dto.getParentResellerId() != null && !dto.getParentResellerId().equals(id)) {
            parentReseller = resellerRepository.findById(dto.getParentResellerId())
                    .orElse(null);
        }

        reseller.setBusinessName(dto.getBusinessName().trim());
        reseller.setContactNo(dto.getContactNo() != null ? dto.getContactNo().trim() : null);
        reseller.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        reseller.setTerritory(dto.getTerritory() != null ? dto.getTerritory().trim() : null);
        reseller.setTier(tier);
        reseller.setParentReseller(parentReseller);
        reseller.setCreditLimit(dto.getCreditLimit() != null ? dto.getCreditLimit() : BigDecimal.ZERO);
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            String newStatus = dto.getStatus().trim().toLowerCase();
            reseller.setStatus(newStatus);
            if (reseller.getUser() != null) {
                reseller.getUser().setIsActive("active".equalsIgnoreCase(newStatus));
            }
        }

        return resellerRepository.save(reseller);
    }

    /**
     * Soft delete: deactivates the reseller via the status field and marks user account inactive.
     */
    @Transactional
    public void softDelete(Integer id) {
        Reseller reseller = findById(id);
        reseller.setStatus("inactive");
        if (reseller.getUser() != null) {
            reseller.getUser().setIsActive(false);
        }
        resellerRepository.save(reseller);
    }

    /**
     * Reactivates a previously soft-deleted reseller.
     */
    @Transactional
    public void restore(Integer id) {
        Reseller reseller = findById(id);
        reseller.setStatus("active");
        if (reseller.getUser() != null) {
            reseller.getUser().setIsActive(true);
        }
        resellerRepository.save(reseller);
    }
}
