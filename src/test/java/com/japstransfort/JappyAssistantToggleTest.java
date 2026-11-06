package com.japstransfort;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JappyAssistantToggleTest {

    private static final Path FRAGMENT_PATH = Path.of("src/main/resources/templates/fragments/help-assistant.html");
    private static final Path JS_PATH = Path.of("src/main/resources/static/js/help-assistant.js");
    private static final Path CSS_PATH = Path.of("src/main/resources/static/css/help-assistant.css");
    private static final Path THEME_CSS_PATH = Path.of("src/main/resources/static/css/theme.css");

    @Test
    @DisplayName("Help assistant template defines accessible toggle, collapse, and unhide buttons")
    void testHelpAssistantTemplateAccessibility() throws IOException {
        String html = Files.readString(FRAGMENT_PATH);

        // Mascot toggle button
        assertTrue(html.contains("id=\"jt-help-btn\""), "Help assistant must have #jt-help-btn");
        assertTrue(html.contains("aria-label=\"Open assistant\""), "Mascot toggle button must have aria-label='Open assistant'");

        // Collapse/Hide button
        assertTrue(html.contains("id=\"jt-help-collapse-btn\""), "Help assistant must have #jt-help-collapse-btn");
        assertTrue(html.contains("aria-label=\"Collapse assistant\""), "Collapse control must have aria-label='Collapse assistant'");

        // Unhide button
        assertTrue(html.contains("id=\"jt-help-unhide-btn\""), "Help assistant must have #jt-help-unhide-btn");
        assertTrue(html.contains("aria-label=\"Unhide assistant\""), "Unhide button must have aria-label='Unhide assistant'");
    }

    @Test
    @DisplayName("JavaScript implements collapsible and hide/unhide persistence with localStorage and public API")
    void testJavaScriptCollapsibleLogic() throws IOException {
        String js = Files.readString(JS_PATH);

        // localStorage keys
        assertTrue(js.contains("jt_jappy_collapsed"), "Must use localStorage key 'jt_jappy_collapsed'");
        assertTrue(js.contains("jt_jappy_hidden"), "Must use localStorage key 'jt_jappy_hidden'");
        assertTrue(js.contains("localStorage.getItem"), "Must read from localStorage");
        assertTrue(js.contains("localStorage.setItem"), "Must write to localStorage");

        // Hidden & Collapsed state toggling
        assertTrue(js.contains("function setHidden(hidden)"), "Must define setHidden function");
        assertTrue(js.contains("function setCollapsed(collapsed)"), "Must define setCollapsed function");
        assertTrue(js.contains("jt-help-btn--collapsed"), "Must apply .jt-help-btn--collapsed class");
        assertTrue(js.contains("aria-label', 'Open assistant'"), "Must update aria-label to 'Open assistant' when collapsed");

        // Public API
        assertTrue(js.contains("collapse: function"), "Public API must expose collapse method");
        assertTrue(js.contains("expand: function"), "Public API must expose expand method");
        assertTrue(js.contains("hide: function"), "Public API must expose hide method");
        assertTrue(js.contains("unhide: function"), "Public API must expose unhide method");

        // Narrow viewport default logic (< 768px)
        assertTrue(js.contains("768"), "Must handle narrow viewport (< 768px) default collapsed logic");
    }

    @Test
    @DisplayName("CSS provides 48px compact button and mobile responsive collapsed styles")
    void testCssCollapsibleAndClearanceStyles() throws IOException {
        String css = Files.readString(CSS_PATH);

        // 48x48px compact button styling
        assertTrue(css.contains(".jt-help-btn--collapsed"), "CSS must define .jt-help-btn--collapsed");
        assertTrue(css.contains("width: 48px;"), "Collapsed button must be 48px wide");
        assertTrue(css.contains("height: 48px;"), "Collapsed button must be 48px high");

        // Unhide button styling
        assertTrue(css.contains(".jt-help-unhide-btn"), "CSS must define .jt-help-unhide-btn");
        assertTrue(css.contains(".jt-help-container--hidden"), "CSS must define .jt-help-container--hidden");

        // Media query for narrow viewports
        assertTrue(css.contains("@media (max-width: 767.98px)"), "CSS must handle narrow viewports");

        // Theme CSS bottom clearance
        String themeCss = Files.readString(THEME_CSS_PATH);
        assertTrue(themeCss.contains("--jt-mascot-clearance"), "theme.css must declare --jt-mascot-clearance");
        assertTrue(themeCss.contains("padding-bottom: calc(26px + var(--jt-mascot-clearance));"),
                "theme.css main must include bottom clearance padding");
    }
}
