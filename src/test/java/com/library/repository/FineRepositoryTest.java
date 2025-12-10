package com.library.repository;

import com.library.model.Fine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FineRepository
 * @author Library Team
 * @version 1.0
 */
class FineRepositoryTest {
    private FineRepository fineRepository;

    @BeforeEach
    void setUp() {
        fineRepository = new FineRepository();
    }

    @Test
    void testCreateFine() {
        Fine fine = fineRepository.createFine("U001", 50.0);

        assertNotNull(fine);
        assertEquals("U001", fine.getUserId());
        assertEquals(50.0, fine.getAmount(), 0.001);
        assertFalse(fine.isPaid());
        assertTrue(fine.getFineId().startsWith("F"));
    }

    @Test
    void testFindFinesByUser() {
        List<Fine> fines = fineRepository.findFinesByUser("U002");

        assertFalse(fines.isEmpty());
        for (Fine fine : fines) {
            assertEquals("U002", fine.getUserId());
        }
    }

    @Test
    void testGetUnpaidFinesByUser() {
        List<Fine> unpaidFines = fineRepository.getUnpaidFinesByUser("U002");

        assertFalse(unpaidFines.isEmpty());
        for (Fine fine : unpaidFines) {
            assertFalse(fine.isPaid());
            assertEquals("U002", fine.getUserId());
        }
    }

    @Test
    void testGetTotalUnpaidAmount() {
        double totalUnpaid = fineRepository.getTotalUnpaidAmount("U002");
        assertTrue(totalUnpaid > 0);
    }

    @Test
    void testMakePaymentSuccess() {
        // Create a new fine for testing payment
        Fine fine = fineRepository.createFine("U001", 30.0);
        String fineId = fine.getFineId();

        Fine.PaymentResult result = fineRepository.makePayment(fineId, 15.0);
        assertTrue(result.isSuccess());

        Fine updatedFine = fineRepository.findFineById(fineId);
        assertEquals(15.0, updatedFine.getPaidAmount(), 0.001);
        assertEquals(15.0, updatedFine.getRemainingBalance(), 0.001);
        assertFalse(updatedFine.isPaid());
    }

    @Test
    void testMakePaymentFullAmount() {
        Fine fine = fineRepository.createFine("U001", 25.0);
        String fineId = fine.getFineId();

        Fine.PaymentResult result = fineRepository.makePayment(fineId, 25.0);
        assertTrue(result.isSuccess());

        Fine paidFine = fineRepository.findFineById(fineId);
        assertTrue(paidFine.isPaid());
        assertEquals(0.0, paidFine.getRemainingBalance(), 0.001);
    }

    @Test
    void testMakePaymentInvalidFineId() {
        Fine.PaymentResult result = fineRepository.makePayment("INVALID_ID", 10.0);
        assertFalse(result.isSuccess());
    }

    @Test
    void testMakePaymentAlreadyPaidFine() {
        // Create and pay a fine
        Fine fine = fineRepository.createFine("U001", 20.0);
        fineRepository.makePayment(fine.getFineId(), 20.0);

        // Try to pay again
        Fine.PaymentResult result = fineRepository.makePayment(fine.getFineId(), 10.0);
        assertFalse(result.isSuccess());
    }

    @Test
    void testFindFineById() {
        Fine newFine = fineRepository.createFine("U001", 40.0);
        Fine foundFine = fineRepository.findFineById(newFine.getFineId());

        assertNotNull(foundFine);
        assertEquals(newFine.getFineId(), foundFine.getFineId());
        assertEquals("U001", foundFine.getUserId());
        assertEquals(40.0, foundFine.getAmount(), 0.001);
    }

    @Test
    void testFindFineByIdNotFound() {
        Fine foundFine = fineRepository.findFineById("NONEXISTENT");
        assertNull(foundFine);
    }

    @Test
    void testGetAllFines() {
        List<Fine> allFines = fineRepository.getAllFines();
        assertFalse(allFines.isEmpty());

        // Should contain at least the sample fines
        boolean hasSampleFines = allFines.stream()
                .anyMatch(fine -> fine.getFineId().equals("F0001") || fine.getUserId().equals("U002"));
        assertTrue(hasSampleFines);
    }

//    @Test
//    void testFineIdIncrement() {
//        Fine fine1 = fineRepository.createFine("U001", 10.0);
//        Fine fine2 = fineRepository.createFine("U001", 20.0);
//        Fine fine3 = fineRepository.createFine("U001", 30.0);
//
//        // All fines should have unique IDs
//        assertNotEquals(fine1.getFineId(), fine2.getFineId());
//        assertNotEquals(fine2.getFineId(), fine3.getFineId());
//        assertNotEquals(fine1.getFineId(), fine3.getFineId());
//    }
}