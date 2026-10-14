package com.japstransfort.controller;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.dto.AccountProfileDto;
import com.japstransfort.dto.ChangePasswordDto;
import com.japstransfort.entity.Reseller;
import com.japstransfort.entity.User;
import com.japstransfort.entity.UserRole;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.UserRepository;
import com.japstransfort.validator.PasswordValidator;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for the account settings page at /account for logged-in users (admin, staff, reseller).
 * Ensures only the authenticated user's record is loaded and updated.
 */
@Controller
@RequestMapping("/account")
public class AccountController {

    private final UserRepository userRepository;
    private final ResellerRepository resellerRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountController(UserRepository userRepository,
                             ResellerRepository resellerRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.resellerRepository = resellerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private User getLoggedInUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    private void prepareModel(Model model, User user, boolean isReseller) {
        model.addAttribute("user", user);
        model.addAttribute("isReseller", isReseller);
        if (isReseller) {
            Reseller reseller = resellerRepository.findByUser_UserId(user.getUserId()).orElse(null);
            model.addAttribute("reseller", reseller);
        }
    }

    private AccountProfileDto buildProfileDto(User user, boolean isReseller) {
        AccountProfileDto dto = new AccountProfileDto();
        dto.setFullName(user.getFullName());
        if (isReseller) {
            Reseller reseller = resellerRepository.findByUser_UserId(user.getUserId()).orElse(null);
            if (reseller != null) {
                dto.setBusinessName(reseller.getBusinessName());
                dto.setContactNo(reseller.getContactNo());
                dto.setTerritory(reseller.getTerritory());
                dto.setAddress(reseller.getAddress());
            }
        }
        return dto;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String showAccount(Authentication authentication, Model model) {
        User currentUser = getLoggedInUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        boolean isReseller = currentUser.getRole() == UserRole.reseller;
        prepareModel(model, currentUser, isReseller);

        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", buildProfileDto(currentUser, isReseller));
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new ChangePasswordDto());
        }

        return "account";
    }

    @PostMapping("/profile")
    @Transactional
    public String updateProfile(@Valid @ModelAttribute("profileForm") AccountProfileDto profileForm,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        User currentUser = getLoggedInUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        boolean isReseller = currentUser.getRole() == UserRole.reseller;

        if (isReseller) {
            if (profileForm.getBusinessName() == null || profileForm.getBusinessName().trim().isEmpty()) {
                bindingResult.rejectValue("businessName", "required", "Business name is required.");
            } else if (profileForm.getBusinessName().trim().length() > 120) {
                bindingResult.rejectValue("businessName", "size", "Business name cannot exceed 120 characters.");
            }

            if (profileForm.getFullName() == null || profileForm.getFullName().trim().isEmpty()) {
                bindingResult.rejectValue("fullName", "required", "Contact person / full name is required.");
            } else if (profileForm.getFullName().trim().length() > 100) {
                bindingResult.rejectValue("fullName", "size", "Contact person cannot exceed 100 characters.");
            }

            if (profileForm.getContactNo() != null && profileForm.getContactNo().trim().length() > 20) {
                bindingResult.rejectValue("contactNo", "size", "Contact number cannot exceed 20 characters.");
            }

            if (profileForm.getTerritory() != null && profileForm.getTerritory().trim().length() > 80) {
                bindingResult.rejectValue("territory", "size", "City/Location cannot exceed 80 characters.");
            }
        } else {
            if (profileForm.getFullName() == null || profileForm.getFullName().trim().isEmpty()) {
                bindingResult.rejectValue("fullName", "required", "Full name is required.");
            } else if (profileForm.getFullName().trim().length() > 100) {
                bindingResult.rejectValue("fullName", "size", "Full name cannot exceed 100 characters.");
            }
        }

        if (bindingResult.hasErrors()) {
            prepareModel(model, currentUser, isReseller);
            if (!model.containsAttribute("passwordForm")) {
                model.addAttribute("passwordForm", new ChangePasswordDto());
            }
            return "account";
        }

        currentUser.setFullName(profileForm.getFullName().trim());
        userRepository.save(currentUser);

        if (isReseller) {
            Reseller reseller = resellerRepository.findByUser_UserId(currentUser.getUserId()).orElse(null);
            if (reseller != null) {
                reseller.setBusinessName(profileForm.getBusinessName().trim());
                reseller.setContactNo(profileForm.getContactNo() != null && !profileForm.getContactNo().trim().isEmpty()
                        ? profileForm.getContactNo().trim() : null);
                reseller.setTerritory(profileForm.getTerritory() != null && !profileForm.getTerritory().trim().isEmpty()
                        ? profileForm.getTerritory().trim() : null);
                reseller.setAddress(profileForm.getAddress() != null && !profileForm.getAddress().trim().isEmpty()
                        ? profileForm.getAddress().trim() : null);
                resellerRepository.save(reseller);
            }
        }

        // Keep session authentication updated with modified details
        CustomUserDetails userDetails = new CustomUserDetails(currentUser);
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                userDetails,
                currentUser.getPasswordHash(),
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        redirectAttributes.addFlashAttribute("profileSuccessMessage", "Profile updated successfully.");
        return "redirect:/account";
    }

    @PostMapping("/password")
    @Transactional
    public String updatePassword(@Valid @ModelAttribute("passwordForm") ChangePasswordDto passwordForm,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        User currentUser = getLoggedInUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        boolean isReseller = currentUser.getRole() == UserRole.reseller;

        // 1. Verify current password with PasswordEncoder
        if (passwordForm.getCurrentPassword() == null || passwordForm.getCurrentPassword().isEmpty()) {
            bindingResult.rejectValue("currentPassword", "required", "Current password is required.");
        } else if (!passwordEncoder.matches(passwordForm.getCurrentPassword(), currentUser.getPasswordHash())) {
            bindingResult.rejectValue("currentPassword", "invalid", "Current password is incorrect.");
        }

        // 2. Verify new password
        if (passwordForm.getNewPassword() == null || passwordForm.getNewPassword().isEmpty()) {
            bindingResult.rejectValue("newPassword", "required", "New password is required.");
        } else {
            // Require new != current
            if (passwordForm.getNewPassword().equals(passwordForm.getCurrentPassword()) ||
                    passwordEncoder.matches(passwordForm.getNewPassword(), currentUser.getPasswordHash())) {
                bindingResult.rejectValue("newPassword", "same", "New password must be different from current password.");
            }

            // Apply shared password rules
            List<String> passwordErrors = PasswordValidator.validate(passwordForm.getNewPassword());
            for (String err : passwordErrors) {
                bindingResult.rejectValue("newPassword", "complexity", err);
            }
        }

        // 3. Require new == confirm
        if (passwordForm.getConfirmPassword() == null || passwordForm.getConfirmPassword().isEmpty()) {
            bindingResult.rejectValue("confirmPassword", "required", "Please confirm your new password.");
        } else if (!passwordForm.getConfirmPassword().equals(passwordForm.getNewPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }

        if (bindingResult.hasErrors()) {
            prepareModel(model, currentUser, isReseller);
            if (!model.containsAttribute("profileForm")) {
                model.addAttribute("profileForm", buildProfileDto(currentUser, isReseller));
            }
            return "account";
        }

        // Store BCrypt
        currentUser.setPasswordHash(passwordEncoder.encode(passwordForm.getNewPassword()));
        userRepository.save(currentUser);

        // Keep user logged in
        CustomUserDetails userDetails = new CustomUserDetails(currentUser);
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                userDetails,
                currentUser.getPasswordHash(),
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        redirectAttributes.addFlashAttribute("passwordSuccessMessage", "Password updated successfully.");
        return "redirect:/account";
    }
}
