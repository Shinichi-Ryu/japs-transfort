package com.japstransfort.service;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GeminiClientTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testMissingApiKeySilentlyUnavailable() {
        GeminiClient client = new GeminiClient(objectMapper, null, "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", null);
        assertFalse(client.isAiAvailable());
        Optional<String> result = client.generateContent("instruction", Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseGeminiResponseSuccess() {
        GeminiClient client = new GeminiClient(objectMapper, "dummy-key", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", null);
        String json = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "thoughtSignature": "ignore-this-hash",
                    "text": "1. Go to Products in the sidebar.\\n2. Click + Add Product."
                  },
                  {
                    "extraField": 123
                  },
                  {
                    "text": "\\n3. Fill in details and click Save."
                  }
                ],
                "role": "model"
              },
              "finishReason": "STOP",
              "avgLogprobs": -0.15
            }
          ],
          "usageMetadata": {
            "promptTokenCount": 150,
            "candidatesTokenCount": 35
          }
        }
        """;

        Optional<String> result = client.parseGeminiResponse(json);
        assertTrue(result.isPresent());
        assertEquals("1. Go to Products in the sidebar.\n2. Click + Add Product.\n3. Fill in details and click Save.", result.get());
    }

    @Test
    void testParseGeminiResponseNotStopFinishReason() {
        GeminiClient client = new GeminiClient(objectMapper, "dummy-key", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", null);
        String json = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  { "text": "Incomplete answer..." }
                ]
              },
              "finishReason": "SAFETY"
            }
          ]
        }
        """;

        Optional<String> result = client.parseGeminiResponse(json);
        assertTrue(result.isEmpty(), "Should fail when finishReason is not STOP");
    }

    @Test
    void testParseGeminiResponseEmptyCandidates() {
        GeminiClient client = new GeminiClient(objectMapper, "dummy-key", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", null);
        String json = """
        {
          "candidates": []
        }
        """;

        Optional<String> result = client.parseGeminiResponse(json);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseGeminiResponseBlankText() {
        GeminiClient client = new GeminiClient(objectMapper, "dummy-key", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", null);
        String json = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  { "text": "   " }
                ]
              },
              "finishReason": "STOP"
            }
          ]
        }
        """;

        Optional<String> result = client.parseGeminiResponse(json);
        assertTrue(result.isEmpty());
    }

    @Test
    void testPauseAiServerWide() {
        GeminiClient client = new GeminiClient(objectMapper, "dummy-key", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com", null);
        assertTrue(client.isAiAvailable());
        client.pauseAi(60000L);
        assertTrue(client.isPaused());
        assertFalse(client.isAiAvailable());
    }
}
