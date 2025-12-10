package com.library.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Fine model
 * @author Library Team
 * @version 1.0
 */
class FineTest {

    @Test
    void testFineCreation() {
        Fine fine = new Fine("F0001", "U001", 25.0);

        assertEquals("F0001", fine.getFineId());
        assertEquals("U001", fine.getUserId());
        assertEquals(25.0, fine.getAmount(), 0.001);
        assertEquals(0.0, fine.getPaidAmount(), 0.001);
        assertFalse(fine.isPaid());
        assertEquals(25.0, fine.getRemainingBalance(), 0.001);
    }

    @Test
    void testFullPayment() {
        Fine fine = new Fine("F0001", "U001", 25.0);
        Fine.PaymentResult result = fine.makePayment(25.0);

        assertTrue(result.isSuccess());
        assertEquals(0.0, result.getRefundAmount(), 0.001);
        assertTrue(fine.isPaid());
        assertEquals(25.0, fine.getPaidAmount(), 0.001);
        assertEquals(0.0, fine.getRemainingBalance(), 0.001);
    }

    @Test
    void testPartialPayment() {
        Fine fine = new Fine("F0001", "U001", 25.0);
        Fine.PaymentResult result = fine.makePayment(10.0);

        assertTrue(result.isSuccess());
        assertEquals(0.0, result.getRefundAmount(), 0.001);
        assertFalse(fine.isPaid());
        assertEquals(10.0, fine.getPaidAmount(), 0.001);
        assertEquals(15.0, fine.getRemainingBalance(), 0.001);
    }

    @Test
    void testOverPayment() {
        Fine fine = new Fine("F0001", "U001", 25.0);
        Fine.PaymentResult result = fine.makePayment(30.0);

        assertTrue(result.isSuccess());
        assertEquals(5.0, result.getRefundAmount(), 0.001);
        assertTrue(fine.isPaid());
        assertEquals(25.0, fine.getPaidAmount(), 0.001);
        assertEquals(0.0, fine.getRemainingBalance(), 0.001);
    }

    @Test
    void testInvalidPaymentZeroAmount() {
        Fine fine = new Fine("F0001", "U001", 25.0);
        Fine.PaymentResult result = fine.makePayment(0.0);

        assertFalse(result.isSuccess());
        assertEquals(0.0, result.getRefundAmount(), 0.001);
        assertFalse(fine.isPaid());
        assertEquals(0.0, fine.getPaidAmount(), 0.001);
    }

    @Test
    void testInvalidPaymentNegativeAmount() {
        Fine fine = new Fine("F0001", "U001", 25.0);
        Fine.PaymentResult result = fine.makePayment(-10.0);

        assertFalse(result.isSuccess());
        assertEquals(0.0, result.getRefundAmount(), 0.001);
        assertFalse(fine.isPaid());
        assertEquals(0.0, fine.getPaidAmount(), 0.001);
    }

    @Test
    void testFineToString() {
        Fine fine = new Fine("F0001", "U001", 25.0);
        String toStringResult = fine.toString();

        assertTrue(toStringResult.contains("F0001"));
        assertTrue(toStringResult.contains("U001"));
        assertTrue(toStringResult.contains("25.00"));
        assertTrue(toStringResult.contains("Unpaid"));
    }
}