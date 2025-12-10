package com.library.model;

import java.time.LocalDate;

/**
 * Represents a media loan in the library system
 * @author Library Team
 * @version 1.0
 */
public class Loan {
    private String loanId;
    private String userId;
    private String mediaId;
    private String mediaType; // "BOOK" or "CD"
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private boolean isOverdue;

    public Loan(String loanId, String userId, String mediaId, String mediaType,
                LocalDate borrowDate, LocalDate dueDate) {
        this.loanId = loanId;
        this.userId = userId;
        this.mediaId = mediaId;
        this.mediaType = mediaType;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = null;
        this.isOverdue = false;
    }

    // Getters and setters
    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public boolean isOverdue() { return isOverdue; }
    public void setOverdue(boolean overdue) { isOverdue = overdue; }

    /**
     * Calculates flat fine for this loan based on media type
     * @return flat fine amount
     */
    public double calculateFlatFine() {
        if (returnDate != null || !isOverdue) {
            return 0.0;
        }

        // Use flat fines based on media type
        if ("BOOK".equals(mediaType)) {
            return 10.00;
        } else if ("CD".equals(mediaType)) {
            return 20.00;
        }
        return 0.0;
    }

    /**
     * Calculates fine - for backward compatibility (calls calculateFlatFine)
     * @param currentDate the current date (unused - kept for compatibility)
     * @return calculated fine amount
     */
    public double calculateFine(LocalDate currentDate) {
        return calculateFlatFine(); // Always returns flat fine, ignores days overdue
    }

    public boolean checkOverdue(LocalDate currentDate) {
        this.isOverdue = currentDate.isAfter(dueDate) && returnDate == null;
        return this.isOverdue;
    }

    @Override
    public String toString() {
        return String.format("Loan ID: %-8s | User: %-6s | Media: %-15s | Type: %-4s | Due: %s | Overdue: %s",
                loanId, userId, mediaId, mediaType, dueDate, isOverdue ? "Yes" : "No");
    }
}