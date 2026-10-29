package com.japstransfort.controller;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.japstransfort.dto.HelpEntryDto;
import com.japstransfort.dto.HelpSearchResponse;
import com.japstransfort.dto.HelpSearchResponse.HelpQuickChip;
import com.japstransfort.dto.HelpSearchResponse.HelpResultItem;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-only Help Assistant Controller.
 * Serves role-aware knowledge base search for the floating Jappy assistant.
 * Strictly decoupled from business database entities; never returns database records.
 */
@RestController
public class HelpController {

    private static final Logger log = LoggerFactory.getLogger(HelpController.class);

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final List<HelpEntryDto> knowledgeBase = new ArrayList<>();

    private static final Set<String> STOP_WORDS = Set.of(
            "how", "do", "i", "can", "to", "the", "a", "an", "is", "are", "in", "on", "at",
            "for", "of", "it", "my", "me", "you", "we", "where", "what", "when", "why",
            "who", "which", "please", "help", "need", "want", "paano", "ano", "saan",
            "kailan", "bakit", "sino", "ang", "mga", "sa", "ng", "ba", "po", "ko", "mo",
            "na", "din", "rin", "yung", "ung", "paki", "pwede", "makita", "mag", "magkano"
    );

    public HelpController(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initKnowledgeBase() {
        try {
            Resource resource = resourceLoader.getResource("classpath:help/knowledge-base.json");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    List<HelpEntryDto> entries = objectMapper.readValue(is, new TypeReference<List<HelpEntryDto>>() {});
                    knowledgeBase.clear();
                    if (entries != null) {
                        knowledgeBase.addAll(entries);
                    }
                    log.info("Loaded {} help entries into Jappy knowledge base.", knowledgeBase.size());
                }
            } else {
                log.warn("Help knowledge-base.json not found in classpath:help/");
            }
        } catch (Exception e) {
            log.error("Failed to load Jappy knowledge base", e);
        }
    }

    @GetMapping("/help/search")
    public ResponseEntity<HelpSearchResponse> search(
            @RequestParam(value = "q", required = false) String query,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userRole = determineUserRole(authentication);
        List<HelpEntryDto> roleEntries = knowledgeBase.stream()
                .filter(entry -> isEligibleForRole(entry, userRole))
                .collect(Collectors.toList());

        List<HelpQuickChip> quickChips = roleEntries.stream()
                .filter(e -> Boolean.TRUE.equals(e.getQuickChip()))
                .map(e -> new HelpQuickChip(e.getId(), e.getTitle(), e.getTitle()))
                .collect(Collectors.toList());

        HelpSearchResponse response = new HelpSearchResponse();
        response.setQuickChips(quickChips);

        String trimmedQuery = query != null ? query.trim() : "";
        response.setQuery(trimmedQuery);

        if (trimmedQuery.isEmpty()) {
            response.setFound(true);
            response.setAvatarMood("hello");
            response.setMessage("Hi! I'm Jappy, your assistant for Japs Transfort. How can I help you today?");
            return ResponseEntity.ok(response);
        }

        String cleaned = cleanQuery(trimmedQuery);
        List<String> tokens = Arrays.stream(cleaned.split("\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !STOP_WORDS.contains(s))
                .collect(Collectors.toList());

        // Score eligible entries
        Map<HelpEntryDto, Integer> scoreMap = new HashMap<>();
        for (HelpEntryDto entry : roleEntries) {
            int score = calculateScore(entry, cleaned, tokens);
            if (score >= 15) { // Meaningful threshold for match
                scoreMap.put(entry, score);
            }
        }

        if (scoreMap.isEmpty()) {
            response.setFound(false);
            response.setAvatarMood("surprised");
            response.setMessage("I couldn't find an answer for that. You can try searching with different keywords, or check out these popular topics:");
            return ResponseEntity.ok(response);
        }

        // Sort descending by score and pick top 1 to 3 results
        List<HelpResultItem> topResults = scoreMap.entrySet().stream()
                .sorted(Map.Entry.<HelpEntryDto, Integer>comparingByValue().reversed())
                .limit(3)
                .map(entry -> {
                    HelpEntryDto dto = entry.getKey();
                    return new HelpResultItem(
                            dto.getId(),
                            dto.getTitle(),
                            dto.getAnswer(),
                            dto.getLink(),
                            dto.getLinkLabel()
                    );
                })
                .collect(Collectors.toList());

        response.setFound(true);
        // If the top result is smalltalk greeting, mood is hello, otherwise happy
        String topId = topResults.get(0).getId();
        if ("smalltalk-hello".equalsIgnoreCase(topId)) {
            response.setAvatarMood("hello");
        } else {
            response.setAvatarMood("happy");
        }
        response.setResults(topResults);

        return ResponseEntity.ok(response);
    }

    private int calculateScore(HelpEntryDto entry, String cleanQuery, List<String> tokens) {
        int score = 0;
        String titleLower = entry.getTitle() != null ? cleanQuery(entry.getTitle()) : "";
        List<String> keywords = entry.getKeywords() != null ? entry.getKeywords() : Collections.emptyList();

        // Exact match on full query against title
        if (titleLower.equals(cleanQuery)) {
            score += 100;
        } else if (titleLower.contains(cleanQuery) && cleanQuery.length() >= 4) {
            score += 50;
        }

        // Full query matches any keyword exactly or partially
        for (String kw : keywords) {
            String kwClean = cleanQuery(kw);
            if (kwClean.equals(cleanQuery)) {
                score += 80;
            } else if (cleanQuery.contains(kwClean) && kwClean.length() >= 4) {
                score += 40;
            } else if (kwClean.contains(cleanQuery) && cleanQuery.length() >= 4) {
                score += 35;
            }
        }

        // Token matching
        for (String token : tokens) {
            if (token.length() < 3) continue;

            if (titleLower.contains(token)) {
                score += 20;
            }

            for (String kw : keywords) {
                String kwClean = cleanQuery(kw);
                if (kwClean.equals(token)) {
                    score += 30;
                } else if (kwClean.startsWith(token) || (token.startsWith(kwClean) && kwClean.length() >= 4)) {
                    score += 18;
                }
            }

            if (entry.getId() != null && entry.getId().toLowerCase(Locale.ROOT).contains(token)) {
                score += 15;
            }
        }

        return score;
    }

    private String cleanQuery(String input) {
        if (input == null) return "";
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String determineUserRole(Authentication authentication) {
        if (authentication == null) return "RESELLER";
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) return "RESELLER";

        for (GrantedAuthority ga : authorities) {
            String auth = ga.getAuthority();
            if ("ROLE_ADMIN".equalsIgnoreCase(auth)) return "ADMIN";
            if ("ROLE_STAFF".equalsIgnoreCase(auth)) return "STAFF";
            if ("ROLE_WAREHOUSE".equalsIgnoreCase(auth)) return "WAREHOUSE";
            if ("ROLE_RESELLER".equalsIgnoreCase(auth)) return "RESELLER";
        }
        return "RESELLER";
    }

    private boolean isEligibleForRole(HelpEntryDto entry, String userRole) {
        if (entry.getRoles() == null || entry.getRoles().isEmpty()) {
            return true;
        }
        for (String r : entry.getRoles()) {
            if ("ALL".equalsIgnoreCase(r) || r.equalsIgnoreCase(userRole)) {
                return true;
            }
            if ("ADMIN".equalsIgnoreCase(userRole) && ("STAFF".equalsIgnoreCase(r) || "WAREHOUSE".equalsIgnoreCase(r))) {
                return true;
            }
        }
        return false;
    }
}
