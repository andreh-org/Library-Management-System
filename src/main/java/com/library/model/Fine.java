package com.library.model;

/**
 * Represents a fine in the library system
 * @author Library Team
 * @version 1.0
 */
public class Fine {
    private String fineId;
    private String userId;
    private String loanId; // NEW: Track which loan caused this fine
    private double amount;
    private double paidAmount;
    private boolean isPaid;

    public Fine(String fineId, String userId, double amount, String loanId) {
        this.fineId = fineId;
        this.userId = userId;
        this.loanId = loanId;
        this.amount = amount;
        this.paidAmount = 0.0;
        this.isPaid = false;
    }

    // For backward compatibility
    public Fine(String fineId, String userId, double amount) {
        this(fineId, userId, amount, null);
    }

    // Getters and setters
    public String getFineId() { return fineId; }
    public void setFineId(String fineId) { this.fineId = fineId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLoanId() { return loanId; } // NEW GETTER
    public void setLoanId(String loanId) { this.loanId = loanId; } // NEW SETTER

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }

    public double getRemainingBalance() {
        return amount - paidAmount;
    }

    /**
     * Makes a payment towards the fine
     * @param paymentAmount the amount to pay
     * @return PaymentResult containing success status and refund amount
     */
    public PaymentResult makePayment(double paymentAmount) {
        if (paymentAmount <= 0) {
            return new PaymentResult(false, 0, "Payment amount must be positive.");
        }

        double remainingBalance = getRemainingBalance();
        double refundAmount = 0;

        if (paymentAmount > remainingBalance) {
            // Overpayment - calculate refund
            refundAmount = paymentAmount - remainingBalance;
            paidAmount = amount; // Pay the full amount
            isPaid = true;
            return new PaymentResult(true, refundAmount,
                    "Fine paid in full. Refund amount: $" + String.format("%.2f", refundAmount));
        } else {
            // Normal payment
            paidAmount += paymentAmount;
            if (paidAmount >= amount) {
                isPaid = true;
                paidAmount = amount; // Prevent overpayment storage
            }
            return new PaymentResult(true, 0, "Payment applied successfully.");
        }
    }

    @Override
    public String toString() {
        return String.format("Fine ID: %-8s | User: %-6s | Loan: %-6s | Amount: $%-6.2f | Paid: $%-6.2f | Remaining: $%-6.2f | Status: %s",
                fineId, userId, loanId != null ? loanId : "N/A", amount, paidAmount, getRemainingBalance(), isPaid ? "Paid" : "Unpaid");
    }

    /**
     * Inner class to represent payment result
     */
    public static class PaymentResult {
        private boolean success;
        private double refundAmount;
        private String message;

        public PaymentResult(boolean success, double refundAmount, String message) {
            this.success = success;
            this.refundAmount = refundAmount;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public double getRefundAmount() { return refundAmount; }
        public String getMessage() { return message; }
    }
}