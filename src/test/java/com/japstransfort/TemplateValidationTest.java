package com.japstransfort;

import com.japstransfort.dto.CategoryDto;
import com.japstransfort.dto.ProductFormDto;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TemplateValidationTest {

    @Test
    void testParseAllTemplates() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        engine.setTemplateResolver(resolver);

        engine.setLinkBuilder(new StandardLinkBuilder() {
            @Override
            protected String computeContextPath(IExpressionContext context, String base, Map<String, Object> parameters) {
                return "";
            }
        });

        String[] templates = {
            "login",
            "dashboard",
            "fragments/navbar",
            "categories/list",
            "categories/form",
            "products/list",
            "products/form"
        };

        MockServletContext servletContext = new MockServletContext();
        StaticWebApplicationContext wac = new StaticWebApplicationContext();
        wac.setServletContext(servletContext);
        wac.refresh();
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);

        for (String template : templates) {
            assertDoesNotThrow(() -> {
                MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
                MockHttpServletResponse response = new MockHttpServletResponse();

                Map<String, Object> model = new HashMap<>();
                model.put("search", "");
                model.put("categoryId", 1);
                model.put("status", "all");
                model.put("sort", "name");
                model.put("dir", "asc");
                model.put("reverseDir", "desc");
                model.put("categories", Collections.emptyList());
                model.put("productsPage", new PageImpl<>(Collections.emptyList()));
                model.put("totalProducts", 0L);
                model.put("totalResellers", 0L);
                model.put("totalTiers", 0L);
                model.put("totalCategories", 0L);
                model.put("activeTab", "products");

                CategoryDto categoryDto = new CategoryDto();
                model.put("categoryDto", categoryDto);
                BindingResult catBindingResult = new BeanPropertyBindingResult(categoryDto, "categoryDto");
                model.put(BindingResult.MODEL_KEY_PREFIX + "categoryDto", catBindingResult);

                ProductFormDto productDto = new ProductFormDto();
                model.put("productDto", productDto);
                BindingResult prodBindingResult = new BeanPropertyBindingResult(productDto, "productDto");
                model.put(BindingResult.MODEL_KEY_PREFIX + "productDto", prodBindingResult);

                RequestContext requestContext = new RequestContext(request, response, servletContext, model);
                request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
                request.setAttribute("org.springframework.web.servlet.support.RequestContext", requestContext);

                // Populate request attributes
                model.forEach(request::setAttribute);

                IWebExchange webExchange = application.buildExchange(request, response);
                WebContext webContext = new WebContext(webExchange, request.getLocale(), model);

                engine.process(template, webContext);
                System.out.println(">>> Verified valid template: " + template);
            }, "Template failed parsing: " + template);
        }
    }
}
