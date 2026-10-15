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
            "products/form",
            "print/invoice",
            "print/receipt"
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
                model.put("springRequestContext", requestContext);
                model.put("thymeleafRequestContext", new org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext(requestContext, request));

                com.japstransfort.dto.CompanyDetailsDto company = new com.japstransfort.dto.CompanyDetailsDto("Japs Transfort", "", "", "", "", "");
                model.put("company", company);

                com.japstransfort.dto.InvoiceDto invoice = new com.japstransfort.dto.InvoiceDto();
                invoice.setCompany(company);
                invoice.setEligible(true);
                invoice.setOrderNo("ORD-0001");
                invoice.setOrderDate(java.time.LocalDateTime.now());
                invoice.setListPriceTotal(java.math.BigDecimal.ZERO);
                invoice.setDiscountAmount(java.math.BigDecimal.ZERO);
                invoice.setTotal(java.math.BigDecimal.ZERO);
                invoice.setAmountPaid(java.math.BigDecimal.ZERO);
                invoice.setBalanceDue(java.math.BigDecimal.ZERO);
                invoice.setDiscountLabel("Tier discount (0%)");
                invoice.setPaymentStatusBadge("UNPAID");
                model.put("invoice", invoice);

                com.japstransfort.dto.PaymentReceiptDto receipt = new com.japstransfort.dto.PaymentReceiptDto();
                receipt.setCompany(company);
                receipt.setReceiptNo("RCT-000001");
                receipt.setPaidAt(java.time.LocalDateTime.now());
                receipt.setAmount(java.math.BigDecimal.ZERO);
                receipt.setRemainingBalance(java.math.BigDecimal.ZERO);
                model.put("receipt", receipt);
                model.put("backUrl", "/admin/orders/1");

                // Populate request attributes
                model.forEach(request::setAttribute);

                IWebExchange webExchange = application.buildExchange(request, response);
                WebContext webContext = new WebContext(webExchange, request.getLocale(), model);

                engine.process(template, webContext);
                System.out.println(">>> Verified valid template: " + template);

                // For print templates, also verify with filled company details, payments present, and missing reference
                if ("print/invoice".equals(template) || "print/receipt".equals(template)) {
                    com.japstransfort.dto.CompanyDetailsDto filledCompany = new com.japstransfort.dto.CompanyDetailsDto(
                            "Japs Transfort Inc.", "123 Main St, Manila", "+63 912 345 6789", "info@japs.com", "123-456-789-000", "Thank you for your business!");
                    model.put("company", filledCompany);

                    invoice.setCompany(filledCompany);
                    invoice.setPayments(java.util.List.of(
                            new com.japstransfort.dto.InvoicePaymentDto(java.time.LocalDateTime.now(), "Cash", null, java.math.BigDecimal.valueOf(100.50)),
                            new com.japstransfort.dto.InvoicePaymentDto(java.time.LocalDateTime.now(), "GCash", "REF-999", java.math.BigDecimal.valueOf(200.00))
                    ));
                    invoice.setItems(java.util.List.of(
                            new com.japstransfort.dto.InvoiceItemDto("SKU-1", "Product 1", "pc", 2, java.math.BigDecimal.valueOf(150), java.math.BigDecimal.valueOf(300))
                    ));

                    receipt.setCompany(filledCompany);
                    receipt.setReferenceNo(null); // missing reference number

                    model.forEach(request::setAttribute);
                    IWebExchange filledExchange = application.buildExchange(request, response);
                    WebContext filledContext = new WebContext(filledExchange, request.getLocale(), model);
                    engine.process(template, filledContext);
                }
            }, "Template failed parsing: " + template);
        }
    }
}
