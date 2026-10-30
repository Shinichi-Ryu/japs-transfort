package com.japstransfort.controller;

import tools.jackson.databind.ObjectMapper;
import com.japstransfort.dto.HelpAskRequest;
import com.japstransfort.dto.HelpAskResponse;
import com.japstransfort.service.GeminiClient;
import com.japstransfort.service.HelpRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HelpAskTest {

    private ObjectMapper objectMapper;
    private HelpRateLimiter rateLimiter;
    private HelpController controllerNoKey;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        rateLimiter = new HelpRateLimiter();
        GeminiClient clientNoKey = new GeminiClient(objectMapper, null, "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", null);
        controllerNoKey = new HelpController(new DefaultResourceLoader(), objectMapper, clientNoKey, rateLimiter);
        controllerNoKey.initKnowledgeBase();
    }

    @Test
    void testUnauthenticatedReturnsUnauthorized() {
        HelpAskRequest req = new HelpAskRequest("how to order", null);
        ResponseEntity<HelpAskResponse> res = controllerNoKey.ask(req, null);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void testBlankMessageReturnsBadRequest() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        ResponseEntity<HelpAskResponse> res1 = controllerNoKey.ask(new HelpAskRequest("", null), auth);
        assertEquals(HttpStatus.BAD_REQUEST, res1.getStatusCode());

        ResponseEntity<HelpAskResponse> res2 = controllerNoKey.ask(new HelpAskRequest("   ", null), auth);
        assertEquals(HttpStatus.BAD_REQUEST, res2.getStatusCode());

        ResponseEntity<HelpAskResponse> res3 = controllerNoKey.ask(new HelpAskRequest(null, null), auth);
        assertEquals(HttpStatus.BAD_REQUEST, res3.getStatusCode());
    }

    @Test
    void testMessageOver500CharsReturnsBadRequest() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        String longMsg = "a".repeat(501);
        ResponseEntity<HelpAskResponse> res = controllerNoKey.ask(new HelpAskRequest(longMsg, null), auth);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test
    void testInvalidHistoryRoleReturnsBadRequest() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        List<HelpAskRequest.ChatMessage> history = List.of(
                new HelpAskRequest.ChatMessage("hacker_role", "hello")
        );
        ResponseEntity<HelpAskResponse> res = controllerNoKey.ask(new HelpAskRequest("paano mag order", history), auth);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test
    void testNullHistoryTextReturnsBadRequest() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        List<HelpAskRequest.ChatMessage> history = List.of(
                new HelpAskRequest.ChatMessage("user", null)
        );
        ResponseEntity<HelpAskResponse> res = controllerNoKey.ask(new HelpAskRequest("paano mag order", history), auth);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test
    void testWithoutApiKeyFallsBackToKeywordHelpSilently() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        HelpAskRequest req = new HelpAskRequest("how do I place an order?", null);
        ResponseEntity<HelpAskResponse> res = controllerNoKey.ask(req, auth);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals("keyword", res.getBody().getSource());
        assertEquals("happy", res.getBody().getMood());
        assertTrue(res.getBody().getAnswer().contains("Add to Cart") || res.getBody().getAnswer().contains("Catalog"));
        assertFalse(res.getBody().getLinks().isEmpty());
        assertEquals("/portal/catalog", res.getBody().getLinks().get(0).getUrl());
    }

    @Test
    void testResellerCannotAccessAdminHelpViaKeyword() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        HelpAskRequest req = new HelpAskRequest("approve reseller application", null);
        ResponseEntity<HelpAskResponse> res = controllerNoKey.ask(req, auth);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals("keyword", res.getBody().getSource());
        // Verify no admin links or admin answers are given to a reseller
        for (HelpAskResponse.HelpLink link : res.getBody().getLinks()) {
            assertFalse(link.getUrl().startsWith("/admin"), "Reseller must never receive /admin links");
        }
        assertFalse(res.getBody().getAnswer().contains("Approve button on the reseller's row"));
    }

    @Test
    void testRateLimitReturnsFriendlyMessageAndKeywordAnswer() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "spammer@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        HelpAskRequest req = new HelpAskRequest("how to place order", null);

        // First 10 questions pass
        for (int i = 0; i < 10; i++) {
            ResponseEntity<HelpAskResponse> res = controllerNoKey.ask(req, auth);
            assertEquals(HttpStatus.OK, res.getStatusCode());
            assertFalse(res.getBody().getAnswer().contains("I need a short break"));
        }

        // 11th question hits rate limit
        ResponseEntity<HelpAskResponse> res11 = controllerNoKey.ask(req, auth);
        assertEquals(HttpStatus.OK, res11.getStatusCode());
        assertNotNull(res11.getBody());
        assertEquals("keyword", res11.getBody().getSource());
        assertTrue(res11.getBody().getAnswer().startsWith("I need a short break, please try again in a minute."));
        // Still provides keyword answer
        assertTrue(res11.getBody().getAnswer().contains("Catalog") || res11.getBody().getAnswer().contains("Cart"));
    }

    @Test
    void testAiMockSuccessAndPrivacyRedaction() {
        AtomicReference<String> capturedInstruction = new AtomicReference<>();
        AtomicReference<List<GeminiClient.GeminiTurn>> capturedTurns = new AtomicReference<>();

        GeminiClient mockClient = new GeminiClient(objectMapper, "test-key", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", null) {
            @Override
            public Optional<String> generateContent(String systemInstructionText, List<GeminiTurn> contents) {
                capturedInstruction.set(systemInstructionText);
                capturedTurns.set(contents);
                return Optional.of("Here are the steps to add a product:\n1. Open Products.\n2. Click Add.");
            }
        };

        HelpController controllerWithAi = new HelpController(new DefaultResourceLoader(), objectMapper, mockClient, new HelpRateLimiter());
        controllerWithAi.initKnowledgeBase();

        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        List<HelpAskRequest.ChatMessage> history = new ArrayList<>();
        history.add(new HelpAskRequest.ChatMessage("user", "My email is test@company.com and phone is +63 917 123 4567."));
        history.add(new HelpAskRequest.ChatMessage("assistant", "How can I help you?"));

        HelpAskRequest req = new HelpAskRequest("How do I add a product? My secret ref is 123456789.", history);
        ResponseEntity<HelpAskResponse> res = controllerWithAi.ask(req, adminAuth);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals("ai", res.getBody().getSource());
        assertEquals("happy", res.getBody().getMood());
        assertEquals("Here are the steps to add a product:\n1. Open Products.\n2. Click Add.", res.getBody().getAnswer());
        assertFalse(res.getBody().getLinks().isEmpty());

        // Verify privacy redaction on captured turns
        assertNotNull(capturedTurns.get());
        for (GeminiClient.GeminiTurn turn : capturedTurns.get()) {
            assertFalse(turn.getText().contains("test@company.com"), "Email must be redacted");
            assertFalse(turn.getText().contains("+63 917 123 4567"), "Phone must be redacted");
            assertFalse(turn.getText().contains("123456789"), "8+ digit sequence must be redacted");
        }
        // Turn 0 (user history) and Turn 2 (current question) had sensitive data that was replaced
        assertTrue(capturedTurns.get().get(0).getText().contains("[removed]"), "User history must contain [removed]");
        assertTrue(capturedTurns.get().get(capturedTurns.get().size() - 1).getText().contains("[removed]"), "Question must contain [removed]");

        // Verify system instruction only has admin/staff help entries for admin user
        assertNotNull(capturedInstruction.get());
        assertTrue(capturedInstruction.get().contains("You are Jappy"));
        assertTrue(capturedInstruction.get().contains("admin-add-product") || capturedInstruction.get().contains("How do I add a product?"));
    }

    @Test
    void testFollowUpUnderstoodViaHistory() {
        AtomicReference<String> capturedInstruction = new AtomicReference<>();

        GeminiClient mockClient = new GeminiClient(objectMapper, "test-key", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", null) {
            @Override
            public Optional<String> generateContent(String systemInstructionText, List<GeminiTurn> contents) {
                capturedInstruction.set(systemInstructionText);
                return Optional.of("After clicking Add Product, fill in the SKU and price.");
            }
        };

        HelpController controllerWithAi = new HelpController(new DefaultResourceLoader(), objectMapper, mockClient, new HelpRateLimiter());
        controllerWithAi.initKnowledgeBase();

        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        List<HelpAskRequest.ChatMessage> history = List.of(
                new HelpAskRequest.ChatMessage("user", "How do I add a product?"),
                new HelpAskRequest.ChatMessage("assistant", "Go to Products in the sidebar and click Add Product.")
        );

        // Short follow-up question
        HelpAskRequest followUpReq = new HelpAskRequest("and then?", history);
        ResponseEntity<HelpAskResponse> res = controllerWithAi.ask(followUpReq, adminAuth);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("ai", res.getBody().getSource());
        // Verify system instruction included the product entry from previous question
        assertNotNull(capturedInstruction.get());
        assertTrue(capturedInstruction.get().contains("How do I add a product?"));
    }

    @Test
    void testSimulated429Fallback() {
        GeminiClient client429 = new GeminiClient(objectMapper, "test-key", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", null);
        // Simulate pause after 429
        client429.pauseAi(60000L);

        HelpController controller429 = new HelpController(new DefaultResourceLoader(), objectMapper, client429, new HelpRateLimiter());
        controller429.initKnowledgeBase();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        HelpAskRequest req = new HelpAskRequest("how to place order", null);
        ResponseEntity<HelpAskResponse> res = controller429.ask(req, auth);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        // Must fallback to keyword help
        assertEquals("keyword", res.getBody().getSource());
        assertEquals("happy", res.getBody().getMood());
        assertTrue(res.getBody().getAnswer().contains("Catalog") || res.getBody().getAnswer().contains("Cart"));
    }

    @Test
    void testLinksGatingForSmalltalkAndLowConfidence() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "reseller@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        // Greetings should have NO links
        HelpAskRequest reqGreeting = new HelpAskRequest("hello", null);
        ResponseEntity<HelpAskResponse> resGreeting = controllerNoKey.ask(reqGreeting, auth);
        assertEquals(HttpStatus.OK, resGreeting.getStatusCode());
        assertTrue(resGreeting.getBody().getLinks().isEmpty(), "Smalltalk greetings must return empty links");

        // Smalltalk thanks should have NO links
        HelpAskRequest reqThanks = new HelpAskRequest("thank you", null);
        ResponseEntity<HelpAskResponse> resThanks = controllerNoKey.ask(reqThanks, auth);
        assertEquals(HttpStatus.OK, resThanks.getStatusCode());
        assertTrue(resThanks.getBody().getLinks().isEmpty(), "Smalltalk thanks must return empty links");

        // Gibberish / low-confidence should have NO links
        HelpAskRequest reqUnknown = new HelpAskRequest("asdfqwerty completely unrelated text xyz", null);
        ResponseEntity<HelpAskResponse> resUnknown = controllerNoKey.ask(reqUnknown, auth);
        assertEquals(HttpStatus.OK, resUnknown.getStatusCode());
        assertTrue(resUnknown.getBody().getLinks().isEmpty(), "Unmatched queries must return empty links");
    }

    @Test
    void testNewAdminHelpEntries() {
        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // a. Unpaid balances / receivables
        HelpAskRequest reqUnpaid = new HelpAskRequest("how to see unpaid balances and receivables", null);
        ResponseEntity<HelpAskResponse> resUnpaid = controllerNoKey.ask(reqUnpaid, adminAuth);
        assertEquals(HttpStatus.OK, resUnpaid.getStatusCode());
        assertFalse(resUnpaid.getBody().getLinks().isEmpty());
        assertEquals("/admin/resellers", resUnpaid.getBody().getLinks().get(0).getUrl());

        // c. Pending commissions
        HelpAskRequest reqCommissions = new HelpAskRequest("how to see pending commissions", null);
        ResponseEntity<HelpAskResponse> resCommissions = controllerNoKey.ask(reqCommissions, adminAuth);
        assertEquals(HttpStatus.OK, resCommissions.getStatusCode());
        assertFalse(resCommissions.getBody().getLinks().isEmpty());
        assertEquals("/admin/commissions?status=pending", resCommissions.getBody().getLinks().get(0).getUrl());

        // d. Low stock items
        HelpAskRequest reqStock = new HelpAskRequest("how to see low stock items", null);
        ResponseEntity<HelpAskResponse> resStock = controllerNoKey.ask(reqStock, adminAuth);
        assertEquals(HttpStatus.OK, resStock.getStatusCode());
        assertFalse(resStock.getBody().getLinks().isEmpty());
        assertEquals("/admin/inventory", resStock.getBody().getLinks().get(0).getUrl());
    }
}
