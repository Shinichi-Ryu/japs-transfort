package com.japstransfort.service;

import com.japstransfort.entity.Order;
import com.japstransfort.entity.OrderMessage;
import com.japstransfort.entity.Reseller;
import com.japstransfort.entity.User;
import com.japstransfort.repository.OrderMessageRepository;
import com.japstransfort.repository.OrderRepository;
import com.japstransfort.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderMessageServiceTest {

    @Mock
    private OrderMessageRepository orderMessageRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    private OrderMessageService orderMessageService;

    @BeforeEach
    void setUp() {
        orderMessageService = new OrderMessageService(orderMessageRepository, orderRepository, userRepository);
    }

    @Test
    void testBlankBodyRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                orderMessageService.sendAsAdmin(1, 1, "   "));
        assertThrows(IllegalArgumentException.class, () ->
                orderMessageService.sendAsAdmin(1, 1, ""));
        assertThrows(IllegalArgumentException.class, () ->
                orderMessageService.sendAsAdmin(1, 1, null));
    }

    @Test
    void testOverLengthBodyRejected() {
        String longBody = "a".repeat(1001);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orderMessageService.sendAsAdmin(1, 1, longBody));
        assertTrue(ex.getMessage().contains("1000 characters"));
    }

    @Test
    void testMaxLengthBodyAccepted() {
        String body1000 = "a".repeat(1000);
        Order order = new Order();
        User admin = new User();

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(userRepository.findById(99)).thenReturn(Optional.of(admin));
        when(orderMessageRepository.save(any(OrderMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderMessage saved = orderMessageService.sendAsAdmin(1, 99, body1000);
        assertNotNull(saved);
        assertEquals(1000, saved.getBody().length());
        assertEquals("ADMIN", saved.getSenderRole());
        assertTrue(saved.isReadByAdmin());
        assertFalse(saved.isReadByReseller());
    }

    @Test
    void testResellerCannotPostToAnotherResellersOrder() {
        Order order = new Order();
        Reseller ownerReseller = new Reseller();
        ownerReseller.setResellerId(10);
        order.setReseller(ownerReseller);

        when(orderRepository.findById(5)).thenReturn(Optional.of(order));

        // Reseller 20 tries to post on Order 5 belonging to Reseller 10
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orderMessageService.sendAsReseller(5, 20, 200, "Hello"));

        assertEquals("Order does not belong to this reseller.", ex.getMessage());
        verify(orderMessageRepository, never()).save(any());
    }

    @Test
    void testResellerCanPostToOwnOrder() {
        Order order = new Order();
        Reseller ownerReseller = new Reseller();
        ownerReseller.setResellerId(10);
        order.setReseller(ownerReseller);

        User resellerUser = new User();
        resellerUser.setUserId(200);

        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        when(userRepository.findById(200)).thenReturn(Optional.of(resellerUser));
        when(orderMessageRepository.save(any(OrderMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderMessage saved = orderMessageService.sendAsReseller(5, 10, 200, "Inquiry about shipment");

        assertNotNull(saved);
        assertEquals("RESELLER", saved.getSenderRole());
        assertEquals("Inquiry about shipment", saved.getBody());
        assertTrue(saved.isReadByReseller(), "Reseller's own message must be read by reseller");
        assertFalse(saved.isReadByAdmin(), "Reseller's message must be unread by admin");
    }

    @Test
    void testAdminSendSetsFlagsCorrectly() {
        Order order = new Order();
        User adminUser = new User();
        adminUser.setUserId(1);

        when(orderRepository.findById(101)).thenReturn(Optional.of(order));
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(orderMessageRepository.save(any(OrderMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderMessage saved = orderMessageService.sendAsAdmin(101, 1, "Your order has been confirmed.");

        assertNotNull(saved);
        assertEquals("ADMIN", saved.getSenderRole());
        assertTrue(saved.isReadByAdmin(), "Admin's own message must be read by admin");
        assertFalse(saved.isReadByReseller(), "Admin's message must be unread by reseller");
    }

    @Test
    void testMarkReadForAdminAndReseller() {
        orderMessageService.markReadFor(101, "ADMIN");
        verify(orderMessageRepository).markAllAsReadForAdmin(101);

        orderMessageService.markReadFor(101, "RESELLER");
        verify(orderMessageRepository).markAllAsReadForReseller(101);
    }

    @Test
    void testGetUnreadCountsMapForAdmin() {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{101, 3L});
        rows.add(new Object[]{102, 1L});
        when(orderMessageRepository.countUnreadGroupedByOrderForAdmin()).thenReturn(rows);

        java.util.Map<Integer, Long> counts = orderMessageService.getUnreadCountsMapForAdmin();
        assertEquals(2, counts.size());
        assertEquals(3L, counts.get(101));
        assertEquals(1L, counts.get(102));
    }

    @Test
    void testGetUnreadCountsMapForReseller() {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{201, 2L});
        when(orderMessageRepository.countUnreadGroupedByOrderForReseller(50)).thenReturn(rows);

        java.util.Map<Integer, Long> counts = orderMessageService.getUnreadCountsMapForReseller(50);
        assertEquals(1, counts.size());
        assertEquals(2L, counts.get(201));

        java.util.Map<Integer, Long> nullCounts = orderMessageService.getUnreadCountsMapForReseller(null);
        assertTrue(nullCounts.isEmpty());
    }

    @Test
    void testCountUnreadForAdmin() {
        when(orderMessageRepository.countUnreadForAdmin()).thenReturn(5L);
        long count = orderMessageService.countUnreadForAdmin();
        assertEquals(5L, count);
        verify(orderMessageRepository).countUnreadForAdmin();
    }

    @Test
    void testCountUnreadForReseller() {
        when(orderMessageRepository.countUnreadForReseller(42)).thenReturn(3L);
        long count = orderMessageService.countUnreadForReseller(42);
        assertEquals(3L, count);
        verify(orderMessageRepository).countUnreadForReseller(42);

        long nullResellerCount = orderMessageService.countUnreadForReseller(null);
        assertEquals(0L, nullResellerCount);
    }

    @Test
    void testCountUnreadMessagesDelegation() {
        when(orderMessageRepository.countUnreadForAdmin()).thenReturn(7L);
        when(orderMessageRepository.countUnreadForReseller(15)).thenReturn(4L);

        assertEquals(7L, orderMessageService.countUnreadMessages("ADMIN", null));
        assertEquals(4L, orderMessageService.countUnreadMessages("RESELLER", 15));
        assertEquals(0L, orderMessageService.countUnreadMessages("UNKNOWN", 15));
    }
}
