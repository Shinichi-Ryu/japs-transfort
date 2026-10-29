package com.japstransfort.controller;

import tools.jackson.databind.ObjectMapper;
import com.japstransfort.dto.HelpSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HelpControllerTest {

    private HelpController controller;

    @BeforeEach
    void setUp() {
        controller = new HelpController(new DefaultResourceLoader(), new ObjectMapper());
        controller.initKnowledgeBase();
    }

    @Test
    void testUnauthenticatedReturnsUnauthorized() {
        ResponseEntity<HelpSearchResponse> response = controller.search("order", null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testAdminSearchAddProduct() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        ResponseEntity<HelpSearchResponse> response = controller.search("how to add product", auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isFound());
        assertEquals("happy", response.getBody().getAvatarMood());
        assertFalse(response.getBody().getResults().isEmpty());
        assertEquals("admin-add-product", response.getBody().getResults().get(0).getId());
    }

    @Test
    void testResellerCannotSeeAdminOnlyEntries() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        // Reseller searching for "approve reseller" should NOT get admin entry
        ResponseEntity<HelpSearchResponse> response = controller.search("approve reseller", auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Either not found or does not contain admin-approve-reseller
        boolean hasAdminEntry = response.getBody().getResults().stream()
                .anyMatch(r -> r.getId().equals("admin-approve-reseller") || r.getId().startsWith("admin-"));
        assertFalse(hasAdminEntry, "Reseller must NOT be able to view admin-only entries");
    }

    @Test
    void testResellerSearchPlaceOrder() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        ResponseEntity<HelpSearchResponse> response = controller.search("place an order", auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isFound());
        assertEquals("happy", response.getBody().getAvatarMood());
        assertEquals("reseller-place-order", response.getBody().getResults().get(0).getId());
    }

    @Test
    void testSmalltalkGreetings() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        ResponseEntity<HelpSearchResponse> response = controller.search("kamusta jappy", auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isFound());
        assertEquals("hello", response.getBody().getAvatarMood());
        assertEquals("smalltalk-hello", response.getBody().getResults().get(0).getId());
    }

    @Test
    void testNoMatchReturnsSurprisedAndQuickChips() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        ResponseEntity<HelpSearchResponse> response = controller.search("quantum teleportation spaceship", auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isFound());
        assertEquals("surprised", response.getBody().getAvatarMood());
        assertTrue(response.getBody().getResults().isEmpty());
        assertFalse(response.getBody().getQuickChips().isEmpty());
    }

    @Test
    void testRoleQuickChipsDifferBetweenAdminAndReseller() {
        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        UsernamePasswordAuthenticationToken resellerAuth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        ResponseEntity<HelpSearchResponse> adminResp = controller.search("", adminAuth);
        ResponseEntity<HelpSearchResponse> resellerResp = controller.search("", resellerAuth);

        assertNotNull(adminResp.getBody());
        assertNotNull(resellerResp.getBody());

        List<String> adminChipIds = adminResp.getBody().getQuickChips().stream()
                .map(HelpSearchResponse.HelpQuickChip::getId).toList();
        List<String> resellerChipIds = resellerResp.getBody().getQuickChips().stream()
                .map(HelpSearchResponse.HelpQuickChip::getId).toList();

        assertTrue(adminChipIds.contains("admin-add-product"));
        assertFalse(resellerChipIds.contains("admin-add-product"));

        assertTrue(resellerChipIds.contains("reseller-place-order"));
        assertFalse(adminChipIds.contains("reseller-place-order"));
    }
}
