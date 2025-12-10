package com.library.repository;

import com.library.model.Loan;
import com.library.model.Media;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository for managing loan data
 * @author Library Team
 * @version 1.0
 */
public class LoanRepository {
    private List<Loan> loans;
    private int loanCounter;
    private MediaRepository mediaRepository;

    public LoanRepository(MediaRepository mediaRepository) {
        this.loans = new ArrayList<>();
        this.mediaRepository = mediaRepository;
        this.loanCounter = 1;
        initializeSampleLoans(); // Add sample loans for testing
    }

    // Default constructor for backward compatibility
    public LoanRepository() {
        this(new MediaRepository());
    }

    /**
     * Adds sample loans for testing - including overdue ones
     */
    private void initializeSampleLoans() {
        // Create some sample overdue loans for our test users
        LocalDate pastDate = LocalDate.now().minusDays(35); // 7 days overdue
        LocalDate pastDate1 = LocalDate.now().minusDays(35); // 7 days overdue

        LocalDate pastDate2 = LocalDate.now().minusDays(40); // 12 days overdue
        LocalDate pastDate3 = LocalDate.now().minusDays(10); // CD overdue (3 days overdue)

        // U002 - Emma Johnson has an overdue book
        Loan overdueBookLoan = new Loan("L0001", "U002", "978-0743273565", "BOOK",
                pastDate, pastDate.plusDays(28));
        overdueBookLoan.setOverdue(true);
        loans.add(overdueBookLoan);

        mediaRepository.updateMediaAvailability("978-0743273565", false);

        Loan overdueBookLoan1 = new Loan("L0004", "U002", "978-0141439518", "BOOK",
                pastDate1, pastDate1.plusDays(28));
        overdueBookLoan1.setOverdue(true);
        loans.add(overdueBookLoan1);

        // Mark the book as unavailable
        mediaRepository.updateMediaAvailability("978-0141439518", false);

        // U004 - Sarah Davis has an overdue book
        Loan overdueBookLoan2 = new Loan("L0002", "U004", "978-0061120084", "BOOK",
                pastDate2, pastDate2.plusDays(28));
        overdueBookLoan2.setOverdue(true);
        loans.add(overdueBookLoan2);

        // Mark the book as unavailable
        mediaRepository.updateMediaAvailability("978-0061120084", false);

        // U001 - John Smith has an overdue CD (3 days overdue)
        Loan overdueCDLoan = new Loan("L0003", "U001", "CD-001", "CD",
                pastDate3, pastDate3.plusDays(7));
        overdueCDLoan.setOverdue(true);
        loans.add(overdueCDLoan);

        // Mark the CD as unavailable
        mediaRepository.updateMediaAvailability("CD-001", false);

        loanCounter = 5; // Set counter to continue from L0004
    }

    /**
     * Creates a new loan for media
     * @param userId the user ID
     * @param mediaId the media identifier
     * @param mediaType the media type
     * @param borrowDate the borrow date
     * @return the created loan
     */
    public Loan createLoan(String userId, String mediaId, String mediaType,
                           LocalDate borrowDate) {
        int loanPeriod = getLoanPeriodForMediaType(mediaType);
        LocalDate dueDate = borrowDate.plusDays(loanPeriod);
        String loanId = "L" + String.format("%04d", loanCounter++);

        Loan newLoan = new Loan(loanId, userId, mediaId, mediaType, borrowDate, dueDate);
        loans.add(newLoan);

        // Mark the media as unavailable when loan is created
        mediaRepository.updateMediaAvailability(mediaId, false);

        return newLoan;
    }

    /**
     * Gets loan period for media type
     * @param mediaType the media type
     * @return loan period in days
     */
    private int getLoanPeriodForMediaType(String mediaType) {
        switch (mediaType.toUpperCase()) {
            case "BOOK": return 28;
            case "CD": return 7;
            default: return 28;
        }
    }

    /**
     * Creates a loan for a book (convenience method)
     */
    public Loan createBookLoan(String userId, String bookIsbn, LocalDate borrowDate) {
        return createLoan(userId, bookIsbn, "BOOK", borrowDate);
    }

    /**
     * Creates a loan for a CD (convenience method)
     */
    public Loan createCDLoan(String userId, String cdCatalogNumber, LocalDate borrowDate) {
        return createLoan(userId, cdCatalogNumber, "CD", borrowDate);
    }

    public List<Loan> findLoansByUser(String userId) {
        return loans.stream()
                .filter(loan -> loan.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Loan> findLoansByMedia(String mediaId) {
        return loans.stream()
                .filter(loan -> loan.getMediaId().equals(mediaId))
                .collect(Collectors.toList());
    }

    public List<Loan> getActiveLoans() {
        return loans.stream()
                .filter(loan -> loan.getReturnDate() == null)
                .collect(Collectors.toList());
    }

    public List<Loan> getOverdueLoans(LocalDate currentDate) {
        return loans.stream()
                .filter(loan -> {
                    loan.checkOverdue(currentDate);
                    return loan.isOverdue() && loan.getReturnDate() == null;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets overdue loans for a specific user
     * @param userId the user ID
     * @param currentDate the current date
     * @return list of overdue loans
     */
    public List<Loan> getOverdueLoansForUser(String userId, LocalDate currentDate) {
        return loans.stream()
                .filter(loan -> loan.getUserId().equals(userId))
                .filter(loan -> {
                    loan.checkOverdue(currentDate);
                    return loan.isOverdue() && loan.getReturnDate() == null;
                })
                .collect(Collectors.toList());
    }

    public boolean returnMedia(String loanId, LocalDate returnDate) {
        Loan loan = findLoanById(loanId);
        if (loan != null && loan.getReturnDate() == null) {
            loan.setReturnDate(returnDate);
            loan.setOverdue(false);

            // Mark the media as available when returned
            mediaRepository.updateMediaAvailability(loan.getMediaId(), true);

            return true;
        }
        return false;
    }

    public Loan findLoanById(String loanId) {
        return loans.stream()
                .filter(loan -> loan.getLoanId().equals(loanId))
                .findFirst()
                .orElse(null);
    }

    public List<Loan> getAllLoans() {
        return new ArrayList<>(loans);
    }

    public MediaRepository getMediaRepository() {
        return mediaRepository;
    }

    /**
     * Calculates total fines for a user across all media types
     * @param userId the user ID
     * @param currentDate the current date
     * @return total fine amount
     */
    public double calculateTotalFinesForUser(String userId, LocalDate currentDate) {
        return loans.stream()
                .filter(loan -> loan.getUserId().equals(userId))
                .filter(loan -> loan.getReturnDate() == null)
                .mapToDouble(loan -> loan.calculateFlatFine())
                .sum();
    }

    /**
     * Gets integrated overdue report for a user with mixed media types
     * @param userId the user ID
     * @param currentDate the current date
     * @return IntegratedOverdueReport object
     */
    public IntegratedOverdueReport getIntegratedOverdueReport(String userId, LocalDate currentDate) {
        return new IntegratedOverdueReport(userId, currentDate);
    }

    /**
     * Inner class to represent integrated overdue report
     */
    public class IntegratedOverdueReport {
        private String userId;
        private LocalDate reportDate;
        private List<Loan> allActiveLoans;
        private List<Loan> overdueActiveLoans;
        private List<Loan> returnedOverdueLoans;
        private double activeFinesTotal;
        private double returnedFinesTotal;

        public IntegratedOverdueReport(String userId, LocalDate reportDate) {
            this.userId = userId;
            this.reportDate = reportDate;
            this.allActiveLoans = new ArrayList<>();
            this.overdueActiveLoans = new ArrayList<>();
            this.returnedOverdueLoans = new ArrayList<>();
            this.activeFinesTotal = 0.0;
            this.returnedFinesTotal = 0.0;
            calculateReport();
        }

        private void calculateReport() {
            // Get all active loans
            allActiveLoans = loans.stream()
                    .filter(loan -> loan.getUserId().equals(userId))
                    .filter(loan -> loan.getReturnDate() == null)
                    .collect(Collectors.toList());

            // Get returned but overdue loans (for fines that still need to be paid)
            returnedOverdueLoans = loans.stream()
                    .filter(loan -> loan.getUserId().equals(userId))
                    .filter(loan -> loan.getReturnDate() != null)
                    .filter(loan -> {
                        // Check if it was overdue when returned
                        LocalDate dueDate = loan.getDueDate();
                        LocalDate returnDate = loan.getReturnDate();
                        return returnDate.isAfter(dueDate);
                    })
                    .collect(Collectors.toList());

            // Check and mark overdue status for active loans
            for (Loan loan : allActiveLoans) {
                loan.checkOverdue(reportDate);
                if (loan.isOverdue()) {
                    overdueActiveLoans.add(loan);
                    // Apply flat fine based on media type
                    double fine = calculateFlatFine(loan);
                    activeFinesTotal += fine;
                }
            }

            // Calculate fines for returned overdue loans
            for (Loan loan : returnedOverdueLoans) {
                double fine = calculateFlatFine(loan);
                returnedFinesTotal += fine;
            }
        }

        /**
         * Calculate flat fine based on media type
         * @param loan the loan
         * @return flat fine amount
         */
        private double calculateFlatFine(Loan loan) {
            Media media = mediaRepository.findMediaById(loan.getMediaId());
            if (media != null) {
                if ("BOOK".equals(media.getMediaType())) {
                    return 10.00; // $10 for books
                } else if ("CD".equals(media.getMediaType())) {
                    return 20.00; // $20 for CDs
                }
            }
            return 0.0;
        }

        public String getUserId() { return userId; }
        public LocalDate getReportDate() { return reportDate; }
        public List<Loan> getAllActiveLoans() { return allActiveLoans; }
        public List<Loan> getOverdueActiveLoans() { return overdueActiveLoans; }
        public List<Loan> getReturnedOverdueLoans() { return returnedOverdueLoans; }
        public double getActiveFinesTotal() { return activeFinesTotal; }
        public double getReturnedFinesTotal() { return returnedFinesTotal; }
        public double getTotalFine() { return activeFinesTotal + returnedFinesTotal; }

        /**
         * Get the fine for a specific loan
         */
        public double getFineForLoan(Loan loan) {
            Media media = mediaRepository.findMediaById(loan.getMediaId());
            if (media != null) {
                if ("BOOK".equals(media.getMediaType())) {
                    return 10.00;
                } else if ("CD".equals(media.getMediaType())) {
                    return 20.00;
                }
            }
            return 0.0;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            // Header with emojis
            sb.append("\n").append("🎯".repeat(50));
            sb.append("\n📊 MIXED MEDIA OVERDUE REPORT");
            sb.append("\n").append("🎯".repeat(50));

            // Active loans section - integrated
            sb.append("\n📖 ACTIVE LOANS: ").append(allActiveLoans.size()).append(" items");

            if (!allActiveLoans.isEmpty()) {
                sb.append("\n").append("-".repeat(100));
                for (Loan loan : allActiveLoans) {
                    String status = loan.isOverdue() ? "⏰ OVERDUE" : "✅ On Time";
                    String mediaType = "BOOK".equals(loan.getMediaType()) ? "📚 BOOK" : "💿 CD";
                    sb.append(String.format("\n   • %s: %-15s | Due: %s | Status: %s",
                            mediaType, loan.getMediaId(), loan.getDueDate(), status));
                }
            }
            sb.append("\n").append("-".repeat(100));

            // Overdue summary section
            sb.append("\n\n").append("=".repeat(100));
            sb.append("\n📋 OVERDUE SUMMARY FOR USER: ").append(userId);
            sb.append("\n").append("=".repeat(100));

            // Combine active overdue loans and returned overdue loans
            List<Loan> allOverdueLoans = new ArrayList<>();
            allOverdueLoans.addAll(overdueActiveLoans);
            allOverdueLoans.addAll(returnedOverdueLoans);

            if (allOverdueLoans.isEmpty()) {
                sb.append("\n✅ No overdue items or unpaid fines.");
            } else {
                sb.append("\n\n📦 OVERDUE ITEMS & UNPAID FINES:");
                sb.append("\n").append("-".repeat(100));

                for (Loan loan : allOverdueLoans) {
                    String mediaIcon = "BOOK".equals(loan.getMediaType()) ? "📚" : "💿";
                    double fine = getFineForLoan(loan);
                    String status = loan.getReturnDate() != null ? " (Returned)" : " (Active)";
                    sb.append(String.format("\n%s Type: %-4s | Media ID: %-15s | Loan: %-8s | Fine: $%.2f%s",
                            mediaIcon, loan.getMediaType(), loan.getMediaId(), loan.getLoanId(), fine, status));
                }
                sb.append("\n").append("-".repeat(100));
                sb.append(String.format("\n💰 TOTAL FINE: $%.2f", getTotalFine()));

                // Show breakdown
                if (activeFinesTotal > 0) {
                    sb.append(String.format("\n   • Active overdue items: $%.2f", activeFinesTotal));
                }
                if (returnedFinesTotal > 0) {
                    sb.append(String.format("\n   • Unpaid fines for returned items: $%.2f", returnedFinesTotal));
                }
            }

            sb.append("\n").append("=".repeat(100));

            return sb.toString();
        }
    }

    /**
     * Gets overdue summary for a user with mixed media types
     * @param userId the user ID
     * @param currentDate the current date
     * @return OverdueSummary object with mixed media details
     */
    public OverdueSummary getOverdueSummaryForUser(String userId, LocalDate currentDate) {
        OverdueSummary summary = new OverdueSummary(userId);

        loans.stream()
                .filter(loan -> loan.getUserId().equals(userId))
                .filter(loan -> {
                    loan.checkOverdue(currentDate);
                    return loan.isOverdue() && loan.getReturnDate() == null;
                })
                .forEach(loan -> {
                    double fine = loan.calculateFlatFine();  // Changed to calculateFlatFine()
                    summary.addOverdueItem(loan.getMediaType(), loan.getMediaId(), fine, loan.getLoanId());
                });

        return summary;
    }

    /**
     * Inner class to represent overdue summary with mixed media
     */
    public static class OverdueSummary {
        private String userId;
        private List<OverdueItem> overdueItems;
        private double totalFine;

        public OverdueSummary(String userId) {
            this.userId = userId;
            this.overdueItems = new ArrayList<>();
            this.totalFine = 0.0;
        }

        public void addOverdueItem(String mediaType, String mediaId, double fine, String loanId) {
            overdueItems.add(new OverdueItem(mediaType, mediaId, fine, loanId));
            totalFine += fine;
        }

        public String getUserId() { return userId; }
        public List<OverdueItem> getOverdueItems() { return overdueItems; }
        public double getTotalFine() { return totalFine; }

        /**
         * Gets overdue items grouped by media type
         * @return formatted string with media type breakdown
         */
        public String getMediaTypeBreakdown() {
            StringBuilder sb = new StringBuilder();

            // Group items by media type
            var books = overdueItems.stream()
                    .filter(item -> "BOOK".equals(item.getMediaType()))
                    .collect(Collectors.toList());

            var cds = overdueItems.stream()
                    .filter(item -> "CD".equals(item.getMediaType()))
                    .collect(Collectors.toList());

            // Calculate totals by type
            double bookTotal = books.stream().mapToDouble(OverdueItem::getFine).sum();
            double cdTotal = cds.stream().mapToDouble(OverdueItem::getFine).sum();

            sb.append("\n📊 MEDIA TYPE BREAKDOWN:");
            sb.append("\n").append("-".repeat(40));

            if (!books.isEmpty()) {
                sb.append(String.format("\n📚 BOOKS (%d items): $%.2f", books.size(), bookTotal));
                for (OverdueItem item : books) {
                    sb.append(String.format("\n   • %s (Loan: %s): $%.2f",
                            item.getMediaId(), item.getLoanId(), item.getFine()));
                }
            }

            if (!cds.isEmpty()) {
                sb.append(String.format("\n💿 CDs (%d items): $%.2f", cds.size(), cdTotal));
                for (OverdueItem item : cds) {
                    sb.append(String.format("\n   • %s (Loan: %s): $%.2f",
                            item.getMediaId(), item.getLoanId(), item.getFine()));
                }
            }

            sb.append("\n").append("-".repeat(40));
            sb.append(String.format("\n💰 TOTAL FINE: $%.2f", totalFine));

            return sb.toString();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n").append("=".repeat(100));
            sb.append("\n📋 OVERDUE SUMMARY FOR USER: ").append(userId);
            sb.append("\n").append("=".repeat(100));

            if (overdueItems.isEmpty()) {
                sb.append("\n✅ No overdue items.");
            } else {
                // Show each overdue item
                sb.append("\n\n📦 OVERDUE ITEMS:");
                sb.append("\n").append("-".repeat(100));

                for (OverdueItem item : overdueItems) {
                    sb.append("\n").append(item);
                }

                // Show media type breakdown
                sb.append("\n").append("-".repeat(100));
                sb.append(getMediaTypeBreakdown());
            }
            sb.append("\n").append("=".repeat(100));
            return sb.toString();
        }

        /**
         * Inner class for overdue item with media type support
         */
        public static class OverdueItem {
            private String mediaType;
            private String mediaId;
            private double fine;
            private String loanId;

            public OverdueItem(String mediaType, String mediaId, double fine, String loanId) {
                this.mediaType = mediaType;
                this.mediaId = mediaId;
                this.fine = fine;
                this.loanId = loanId;
            }

            public String getMediaType() { return mediaType; }
            public String getMediaId() { return mediaId; }
            public double getFine() { return fine; }
            public String getLoanId() { return loanId; }

            @Override
            public String toString() {
                String mediaIcon = "BOOK".equals(mediaType) ? "📚" : "💿";
                return String.format("%s Type: %-4s | Media ID: %-15s | Loan: %-8s | Fine: $%.2f",
                        mediaIcon, mediaType, mediaId, loanId, fine);
            }
        }
    }
}