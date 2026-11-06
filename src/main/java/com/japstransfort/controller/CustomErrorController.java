package com.japstransfort.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom ErrorController that intercepts /error requests.
 * Renders themed, human-readable error pages for browser requests and returns
 * structured JSON responses for API/AJAX requests.
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, HttpServletResponse response, Model model) {
        int statusCode = resolveStatusCode(request);
        response.setStatus(statusCode);

        if (isJsonRequest(request)) {
            HttpStatus status = HttpStatus.resolve(statusCode);
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            Map<String, Object> body = new HashMap<>();
            body.put("status", statusCode);
            body.put("error", status.getReasonPhrase());
            if (statusCode == 403) {
                body.put("message", "Your session expired. Please sign in again.");
            } else {
                Object errAttr = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
                body.put("message", errAttr != null && !errAttr.toString().isBlank()
                        ? errAttr.toString()
                        : status.getReasonPhrase());
            }
            return ResponseEntity.status(status).body(body);
        }

        model.addAttribute("statusCode", statusCode);

        if (statusCode == 403) {
            model.addAttribute("title", "Access Denied");
            model.addAttribute("message", "Your session expired. Please sign in again.");
            return "error/403";
        } else if (statusCode == 404) {
            model.addAttribute("title", "Page Not Found");
            model.addAttribute("message", "The page you are looking for doesn't exist or has been moved.");
            return "error/404";
        } else if (statusCode == 500) {
            model.addAttribute("title", "Server Error");
            model.addAttribute("message", "Something went wrong on our end. Please try again later.");
            return "error/500";
        } else {
            model.addAttribute("title", "Error " + statusCode);
            model.addAttribute("message", "An unexpected error occurred. Please try again.");
            return "error/error";
        }
    }

    private int resolveStatusCode(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status == null) {
            status = request.getAttribute("javax.servlet.error.status_code");
        }
        if (status != null) {
            try {
                return Integer.parseInt(status.toString());
            } catch (NumberFormatException ignored) {}
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();
        String requestedWith = request.getHeader("X-Requested-With");
        String uri = request.getRequestURI();

        return (accept != null && accept.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (uri != null && uri.startsWith("/api/"));
    }
}
