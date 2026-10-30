package com.japstransfort.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service to call the Google Gemini API for Jappy assistant.
 * Features:
 * - 15-second connect and read timeouts
 * - Server-wide 60s pause upon HTTP 429 / RESOURCE_EXHAUSTED
 * - Strictly keeps API key on server and never logs it
 * - Tolerant response parsing ignoring thoughtSignature, usageMetadata, etc.
 */
@Service
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    // Server-wide pause timestamp after a 429 / RESOURCE_EXHAUSTED
    private final AtomicLong pauseAiUntil = new AtomicLong(0);

    public GeminiClient() {
        this(new ObjectMapper());
    }

    @Autowired
    public GeminiClient(ObjectMapper objectMapper) {
        this(
                objectMapper,
                resolveEnvOrProp("GEMINI_API_KEY", null),
                resolveEnvOrProp("GEMINI_MODEL", "gemini-3.5-flash-lite"),
                resolveEnvOrProp("GEMINI_BASE_URL", "https://generativelanguage.googleapis.com"),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
        );
    }

    public GeminiClient(ObjectMapper objectMapper, String apiKey, String model, String baseUrl, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.apiKey = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : null;
        this.model = (model != null && !model.isBlank()) ? model.trim() : "gemini-3.5-flash-lite";
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "https://generativelanguage.googleapis.com";
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

        if (this.apiKey != null) {
            log.info("GeminiClient initialized with model: {}", this.model);
        } else {
            log.info("GeminiClient initialized without GEMINI_API_KEY. Jappy will use keyword search fallback.");
        }
    }

    private static String resolveEnvOrProp(String name, String def) {
        String val = System.getenv(name);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        val = System.getProperty(name);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        return def;
    }

    /**
     * Checks if the Gemini AI service is available (key is set and not in 429 pause).
     */
    public boolean isAiAvailable() {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        return System.currentTimeMillis() >= pauseAiUntil.get();
    }

    /**
     * Pauses AI calls server-wide for the specified duration in milliseconds.
     */
    public void pauseAi(long durationMillis) {
        pauseAiUntil.set(System.currentTimeMillis() + durationMillis);
    }

    public boolean isPaused() {
        return System.currentTimeMillis() < pauseAiUntil.get();
    }

    /**
     * Sends prompt to Gemini generateContent endpoint.
     *
     * @param systemInstructionText system prompt with role instructions and help entries
     * @param contents history and current question with roles "user" and "model"
     * @return generated answer text, or empty if failed/paused/quota exhausted/blank
     */
    public Optional<String> generateContent(String systemInstructionText, List<GeminiTurn> contents) {
        if (!isAiAvailable()) {
            return Optional.empty();
        }

        try {
            // Build JSON payload
            ObjectNode root = objectMapper.createObjectNode();

            // systemInstruction
            if (systemInstructionText != null && !systemInstructionText.isBlank()) {
                ObjectNode sysInstructionNode = root.putObject("systemInstruction");
                ArrayNode sysParts = sysInstructionNode.putArray("parts");
                sysParts.addObject().put("text", systemInstructionText);
            }

            // contents
            ArrayNode contentsArray = root.putArray("contents");
            if (contents != null) {
                for (GeminiTurn turn : contents) {
                    if (turn == null || turn.getText() == null || turn.getText().isBlank()) {
                        continue;
                    }
                    ObjectNode turnNode = contentsArray.addObject();
                    turnNode.put("role", turn.getRole()); // "user" or "model"
                    ArrayNode parts = turnNode.putArray("parts");
                    parts.addObject().put("text", turn.getText());
                }
            }

            // generationConfig
            ObjectNode genConfig = root.putObject("generationConfig");
            genConfig.put("temperature", 0.3);
            genConfig.put("maxOutputTokens", 600);

            String requestBodyJson = objectMapper.writeValueAsString(root);
            String endpointUrl = baseUrl + "/v1beta/models/" + model + ":generateContent";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();

            if (statusCode == 429) {
                pauseAi(60_000L);
                log.warn("Gemini returned HTTP 429 (Too Many Requests). Pausing AI calls server-wide for 60 seconds.");
                return Optional.empty();
            }

            if (statusCode == 404) {
                log.warn("Gemini model '{}' not found or retired (HTTP 404). Falling back to keyword help.", model);
                return Optional.empty();
            }

            String responseBody = response.body();

            if (statusCode != 200) {
                if (responseBody != null && responseBody.contains("RESOURCE_EXHAUSTED")) {
                    pauseAi(60_000L);
                    log.warn("Gemini returned RESOURCE_EXHAUSTED. Pausing AI calls server-wide for 60 seconds.");
                } else {
                    log.warn("Gemini returned HTTP {}. Falling back to keyword help.", statusCode);
                }
                return Optional.empty();
            }

            if (responseBody == null || responseBody.isBlank()) {
                log.warn("Gemini returned empty body. Falling back to keyword help.");
                return Optional.empty();
            }

            return parseGeminiResponse(responseBody);

        } catch (HttpTimeoutException e) {
            log.warn("Gemini request timed out after 15 seconds. Falling back to keyword help.");
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Gemini call interrupted. Falling back to keyword help.");
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Gemini call encountered unexpected error ({}). Falling back to keyword help.", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * Parses candidates[0].content.parts[] and concatenates ONLY parts with "text".
     * Checks finishReason is "STOP". Tolerate missing/extra fields without throwing.
     */
    public Optional<String> parseGeminiResponse(String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode candidates = rootNode.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                log.warn("Gemini response missing or empty candidates. Falling back to keyword help.");
                return Optional.empty();
            }

            JsonNode candidate0 = candidates.get(0);
            String finishReason = candidate0.path("finishReason").asText("");
            if (!"STOP".equalsIgnoreCase(finishReason)) {
                log.warn("Gemini candidate finishReason is '{}' (not STOP). Falling back to keyword help.", finishReason);
                return Optional.empty();
            }

            JsonNode parts = candidate0.path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                log.warn("Gemini candidate has no content parts. Falling back to keyword help.");
                return Optional.empty();
            }

            StringBuilder sb = new StringBuilder();
            for (JsonNode part : parts) {
                if (part.hasNonNull("text")) {
                    sb.append(part.get("text").asText());
                }
            }

            String answer = sb.toString().trim();
            if (answer.isEmpty()) {
                log.warn("Gemini candidate text is blank. Falling back to keyword help.");
                return Optional.empty();
            }

            return Optional.of(answer);

        } catch (Exception e) {
            log.warn("Failed to parse Gemini response JSON. Falling back to keyword help.", e);
            return Optional.empty();
        }
    }

    public static class GeminiTurn {
        private final String role;
        private final String text;

        public GeminiTurn(String role, String text) {
            this.role = role;
            this.text = text;
        }

        public String getRole() {
            return role;
        }

        public String getText() {
            return text;
        }
    }
}
