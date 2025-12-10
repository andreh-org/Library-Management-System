package com.library.repository;

import com.library.model.Fine;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository for managing fine data
 * @author Library Team
 * @version 1.0
 */
public class FineRepository {
    private List<Fine> fines;
    private int fineCounter;

    public FineRepository() {
        this.fines = new ArrayList<>();
        this.fineCounter = 1;
        initializeSampleFines();
    }

    /**
     * Adds sample fines for testing
     */
    private void initializeSampleFines() {
        // Create fines for two users
        createFine("U002", 20.0, "L0001"); // Emma Johnson has $25 fine for loan L0001
        createFine("U002", 20.0, "L0004");
        createFine("U004", 20.0, "L0002"); // Sarah Davis has $40 fine for loan L0002
    }

    public Fine createFine(String userId, double amount, String loanId) {
        String fineId = "F" + String.format("%04d", fineCounter++);
        Fine newFine = new Fine(fineId, userId, amount, loanId);
        fines.add(newFine);
        return newFine;
    }

    // Overloaded method for backward compatibility
    public Fine createFine(String userId, double amount) {
        return createFine(userId, amount, null);
    }

    public List<Fine> findFinesByUser(String userId) {
        return fines.stream()
                .filter(fine -> fine.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Fine> getUnpaidFinesByUser(String userId) {
        return fines.stream()
                .filter(fine -> fine.getUserId().equals(userId) && !fine.isPaid())
                .collect(Collectors.toList());
    }

    public double getTotalUnpaidAmount(String userId) {
        return getUnpaidFinesByUser(userId).stream()
                .mapToDouble(Fine::getRemainingBalance)
                .sum();
    }

    public Fine.PaymentResult makePayment(String fineId, double paymentAmount) {
        Fine fine = findFineById(fineId);
        if (fine != null && !fine.isPaid()) {
            return fine.makePayment(paymentAmount);
        }
        return new Fine.PaymentResult(false, 0, "Fine not found or already paid.");
    }

    public Fine findFineById(String fineId) {
        return fines.stream()
                .filter(fine -> fine.getFineId().equals(fineId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a fine by loan ID
     */
    public Fine findFineByLoanId(String loanId) {
        return fines.stream()
                .filter(fine -> loanId.equals(fine.getLoanId()))
                .findFirst()
                .orElse(null);
    }

    public List<Fine> getAllFines() {
        return new ArrayList<>(fines);
    }
}