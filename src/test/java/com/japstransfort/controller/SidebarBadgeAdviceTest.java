package com.japstransfort.controller;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.entity.Reseller;
import com.japstransfort.entity.User;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.UserRepository;
import com.japstransfort.service.OrderMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SidebarBadgeAdviceTest {

    @Mock
    private OrderMessageService orderMessageService;

    @Mock
    private ResellerRepository resellerRepository;

    @Mock
    private UserRepository userRepository;

    private SidebarBadgeAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new SidebarBadgeAdvice(orderMessageService, resellerRepository, userRepository);
    }

    @Test
    void testNullAuthenticationReturnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        Long count = advice.unreadMessageCount(null, request);
        assertNull(count);
    }

    @Test
    void testAnonymousAuthenticationReturnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        AnonymousAuthenticationToken auth = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        Long count = advice.unreadMessageCount(auth, request);
        assertNull(count);
    }

    @Test
    void testLoginRegisterErrorPagesReturnNull() {
        User adminUser = new User();
        CustomUserDetails userDetails = new CustomUserDetails(adminUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        MockHttpServletRequest loginReq = new MockHttpServletRequest("GET", "/login");
        assertNull(advice.unreadMessageCount(auth, loginReq));

        MockHttpServletRequest regReq = new MockHttpServletRequest("GET", "/register");
        assertNull(advice.unreadMessageCount(auth, regReq));

        MockHttpServletRequest errReq = new MockHttpServletRequest("GET", "/error");
        assertNull(advice.unreadMessageCount(auth, errReq));
    }

    @Test
    void testAdminReturnsCount() {
        User adminUser = new User();
        CustomUserDetails userDetails = new CustomUserDetails(adminUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        when(orderMessageService.countUnreadForAdmin()).thenReturn(12L);

        Long count = advice.unreadMessageCount(auth, request);
        assertNotNull(count);
        assertEquals(12L, count);
        verify(orderMessageService).countUnreadForAdmin();
    }

    @Test
    void testResellerReturnsCount() {
        User resellerUser = new User();
        resellerUser.setUserId(99);
        CustomUserDetails userDetails = new CustomUserDetails(resellerUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_RESELLER")));

        Reseller reseller = new Reseller();
        reseller.setResellerId(55);
        when(resellerRepository.findByUser_UserId(99)).thenReturn(Optional.of(reseller));
        when(orderMessageService.countUnreadForReseller(55)).thenReturn(4L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/portal/orders");
        Long count = advice.unreadMessageCount(auth, request);
        assertNotNull(count);
        assertEquals(4L, count);
        verify(orderMessageService).countUnreadForReseller(55);
    }

    @Test
    void testExceptionReturnsZeroNeverBreaksPage() {
        User adminUser = new User();
        CustomUserDetails userDetails = new CustomUserDetails(adminUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        when(orderMessageService.countUnreadForAdmin()).thenThrow(new RuntimeException("DB down"));

        Long count = advice.unreadMessageCount(auth, request);
        assertNotNull(count);
        assertEquals(0L, count);
    }
}
