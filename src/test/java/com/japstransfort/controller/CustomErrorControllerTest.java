package com.japstransfort.controller;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomErrorControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CustomErrorController controller = new CustomErrorController();

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        viewResolver.setCharacterEncoding("UTF-8");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    @DisplayName("403 error renders custom themed view with 'Back to Login' and no Whitelabel text")
    void test403RendersCustomThemedView() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"))
                .andExpect(content().string(containsString("Back to Login")))
                .andExpect(content().string(containsString("Your session expired. Please sign in again.")))
                .andExpect(content().string(containsString("Japs Transfort")))
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))));
    }

    @Test
    @DisplayName("404 error renders custom themed 404 view")
    void test404RendersCustomView() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("Page Not Found")))
                .andExpect(content().string(containsString("Back to Login")))
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))));
    }

    @Test
    @DisplayName("500 error renders custom themed 500 view")
    void test500RendersCustomView() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"))
                .andExpect(content().string(containsString("Server Error")))
                .andExpect(content().string(containsString("Back to Login")))
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))));
    }

    @Test
    @DisplayName("Generic fallback renders custom error view")
    void testGenericFallbackError() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 502))
                .andExpect(status().isBadGateway())
                .andExpect(view().name("error/error"))
                .andExpect(content().string(containsString("Back to Login")))
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))));
    }

    @Test
    @DisplayName("API / JSON request to /error receives structured JSON without HTML")
    void testApiJsonError() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.APPLICATION_JSON)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Your session expired. Please sign in again."));
    }
}
