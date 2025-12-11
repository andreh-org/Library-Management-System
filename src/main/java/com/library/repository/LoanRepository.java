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
        initializeSampleLoans();
    }

    public LoanRepository() {
        this(new MediaRepository());
    }

    private void initializeSampleLoans() {
        LocalDate pastDate = LocalDate.now().minusDays(35);
        LocalDate pastDate1 = LocalDate.now().minusDays(35);
        LocalDate pastDate2 = LocalDate.now().minusDays(40);
        LocalDate pastDate3 = LocalDate.now().minusDays(10);

        Loan overdueBookLoan = new Loan("L0001", "U002", "978-0743273565", "BOOK",
                pastDate, pastDate.plusDays(28));
        overdueBookLoan.setOverdue(true);
        loans.add(overdueBookLoan);
        mediaRepository.updateMediaAvailability("978-0743273565", false);

        Loan overdueBookLoan1 = new Loan("L0004", "U002", "978-0141439518", "BOOK",
                pastDate1, pastDate1.plusDays(28));
        overdueBookLoan1.setOverdue(true);
        loans.add(overdueBookLoan1);
        mediaRepository.updateMediaAvailability("978-0141439518", false);

        Loan overdueBookLoan2 = new Loan("L0002", "U004", "978-0061120084", "BOOK",
                pastDate2, pastDate2.plusDays(28));
        overdueBookLoan2.setOverdue(true);
        loans.add(overdueBookLoan2);
        mediaRepository.updateMediaAvailability("978-0061120084", false);

        Loan overdueCDLoan = new Loan("L0003", "U001", "CD-001", "CD",
                pastDate3, pastDate3.plusDays(7));
        overdueCDLoan.setOverdue(true);
        loans.add(overdueCDLoan);
        mediaRepository.updateMediaAvailability("CD-001", false);

        loanCounter = 5;
    }

    public Loan createLoan(String userId, String mediaId, String mediaType,
                           LocalDate borrowDate) {
        int loanPeriod = getLoanPeriodForMediaType(mediaType);
        LocalDate dueDate = borrowDate.plusDays(loanPeriod);
        String loanId = "L" + String.format("%04d", loanCounter++);

        Loan newLoan = new Loan(loanId, userId, mediaId, mediaType, borrowDate, dueDate);
        loans.add(newLoan);
        mediaRepository.updateMediaAvailability(mediaId, false);

        return newLoan;
    }

    private int getLoanPeriodForMediaType(String mediaType) {
        switch (mediaType.toUpperCase()) {
            case "BOOK": return 28;
            case "CD": return 7;
            default: return 28;
        }
    }

    public Loan createBookLoan(String userId, String bookIsbn, LocalDate borrowDate) {
        return createLoan(userId, bookIsbn, "BOOK", borrowDate);
    }

    public Loan createCDLoan(String userId, String cdCatalogNumber, LocalDate borrowDate) {
        return createLoan(userId, cdCatalogNumber, "CD", borrowDate);
    }

    public List<Loan> findLoansByUser(String userId) {
        return loans.stream()
                .filter(loan -> loan.getUserId().equals(userId))
                .toList();
    }

    public List<Loan> findLoansByMedia(String mediaId) {
        return loans.stream()
                .filter(loan -> loan.getMediaId().equals(mediaId))
                .toList();
    }

    public List<Loan> getActiveLoans() {
        return loans.stream()
                .filter(loan -> loan.getReturnDate() == null)
                .toList();
    }

    public List<Loan> getOverdueLoans(LocalDate currentDate) {
        return loans.stream()
                .filter(loan -> {
                    loan.checkOverdue(currentDate);
                    return loan.isOverdue() && loan.getReturnDate() == null;
                })
                .toList();
    }

    public List<Loan> getOverdueLoansForUser(String userId, LocalDate currentDate) {
        return loans.stream()
                .filter(loan -> loan.getUserId().equals(userId))
                .filter(loan -> {
                    loan.checkOverdue(currentDate);
                    return loan.isOverdue() && loan.getReturnDate() == null;
                })
                .toList();
    }

    public boolean returnMedia(String loanId, LocalDate returnDate) {
        Loan loan = findLoanById(loanId);
        if (loan != null && loan.getReturnDate() == null) {
            loan.setReturnDate(returnDate);
            loan.setOverdue(false);
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

    public double calculateTotalFinesForUser(String userId, LocalDate currentDate) {
        return loans.stream()
                .filter(loan -> loan.getUserId().equals(userId))
                .filter(loan -> loan.getReturnDate() == null)
                .mapToDouble(loan -> loan.calculateFlatFine())
                .sum();
    }

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
            allActiveLoans = new ArrayList<>(loans.stream()
                    .filter(loan -> loan.getUserId().equals(userId))
                    .filter(loan -> loan.getReturnDate() == null)
                    .toList());

            returnedOverdueLoans = new ArrayList<>(loans.stream()
                    .filter(loan -> loan.getUserId().equals(userId))
                    .filter(loan -> loan.getReturnDate() != null)
                    .filter(loan -> {
                        LocalDate dueDate = loan.getDueDate();
                        LocalDate returnDate = loan.getReturnDate();
                        return returnDate.isAfter(dueDate);
                    })
                    .toList());

            for (Loan loan : allActiveLoans) {
                loan.checkOverdue(reportDate);
                if (loan.isOverdue()) {
                    overdueActiveLoans.add(loan);
                    double fine = calculateFlatFine(loan);
                    activeFinesTotal += fine;
                }
            }

            for (Loan loan : returnedOverdueLoans) {
                double fine = calculateFlatFine(loan);
                returnedFinesTotal += fine;
            }
        }

        private double calculateFlatFine(Loan loan) {
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

        public String getUserId() { return userId; }
        public LocalDate getReportDate() { return reportDate; }
        public List<Loan> getAllActiveLoans() { return allActiveLoans; }
        public List<Loan> getOverdueActiveLoans() { return overdueActiveLoans; }
        public List<Loan> getReturnedOverdueLoans() { return returnedOverdueLoans; }
        public double getActiveFinesTotal() { return activeFinesTotal; }
        public double getReturnedFinesTotal() { return returnedFinesTotal; }
        public double getTotalFine() { return activeFinesTotal + returnedFinesTotal; }

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

            appendHeader(sb);
            appendActiveLoansSection(sb);
            appendOverdueSummarySection(sb);

            return sb.toString();
        }

        private void appendHeader(StringBuilder sb) {
            sb.append("\n").append("🎯".repeat(50));
            sb.append("\n📊 MIXED MEDIA OVERDUE REPORT");
            sb.append("\n").append("🎯".repeat(50));
        }

        private void appendActiveLoansSection(StringBuilder sb) {
            sb.append("\n📖 ACTIVE LOANS: ").append(allActiveLoans.size()).append(" items");

            if (!allActiveLoans.isEmpty()) {
                sb.append("\n").append("-".repeat(100));
                for (Loan loan : allActiveLoans) {
                    appendActiveLoanDetails(sb, loan);
                }
            }
            sb.append("\n").append("-".repeat(100));
        }

        private void appendActiveLoanDetails(StringBuilder sb, Loan loan) {
            String status = loan.isOverdue() ? "⏰ OVERDUE" : "✅ On Time";
            String mediaType = "BOOK".equals(loan.getMediaType()) ? "📚 BOOK" : "💿 CD";
            sb.append(String.format("\n   • %s: %-15s | Due: %s | Status: %s",
                    mediaType, loan.getMediaId(), loan.getDueDate(), status));
        }

        private void appendOverdueSummarySection(StringBuilder sb) {
            sb.append("\n\n").append("=".repeat(100));
            sb.append("\n📋 OVERDUE SUMMARY FOR USER: ").append(userId);
            sb.append("\n").append("=".repeat(100));

            List<Loan> allOverdueLoans = combineOverdueLoans();

            if (allOverdueLoans.isEmpty()) {
                sb.append("\n✅ No overdue items or unpaid fines.");
            } else {
                appendOverdueItems(sb, allOverdueLoans);
                appendFineSummary(sb);
            }

            sb.append("\n").append("=".repeat(100));
        }

        private List<Loan> combineOverdueLoans() {
            List<Loan> allOverdueLoans = new ArrayList<>();
            allOverdueLoans.addAll(overdueActiveLoans);
            allOverdueLoans.addAll(returnedOverdueLoans);
            return allOverdueLoans;
        }

        private void appendOverdueItems(StringBuilder sb, List<Loan> allOverdueLoans) {
            sb.append("\n\n📦 OVERDUE ITEMS & UNPAID FINES:");
            sb.append("\n").append("-".repeat(100));

            for (Loan loan : allOverdueLoans) {
                appendOverdueLoanDetails(sb, loan);
            }
            sb.append("\n").append("-".repeat(100));
        }

        private void appendOverdueLoanDetails(StringBuilder sb, Loan loan) {
            String mediaIcon = "BOOK".equals(loan.getMediaType()) ? "📚" : "💿";
            double fine = getFineForLoan(loan);
            String status = loan.getReturnDate() != null ? " (Returned)" : " (Active)";
            sb.append(String.format("\n%s Type: %-4s | Media ID: %-15s | Loan: %-8s | Fine: $%.2f%s",
                    mediaIcon, loan.getMediaType(), loan.getMediaId(), loan.getLoanId(), fine, status));
        }

        private void appendFineSummary(StringBuilder sb) {
            sb.append(String.format("\n💰 TOTAL FINE: $%.2f", getTotalFine()));

            if (activeFinesTotal > 0) {
                sb.append(String.format("\n   • Active overdue items: $%.2f", activeFinesTotal));
            }
            if (returnedFinesTotal > 0) {
                sb.append(String.format("\n   • Unpaid fines for returned items: $%.2f", returnedFinesTotal));
            }
        }
    }

    public OverdueSummary getOverdueSummaryForUser(String userId, LocalDate currentDate) {
        OverdueSummary summary = new OverdueSummary(userId);

        loans.stream()
                .filter(loan -> loan.getUserId().equals(userId))
                .filter(loan -> {
                    loan.checkOverdue(currentDate);
                    return loan.isOverdue() && loan.getReturnDate() == null;
                })
                .forEach(loan -> {
                    double fine = loan.calculateFlatFine();
                    summary.addOverdueItem(loan.getMediaType(), loan.getMediaId(), fine, loan.getLoanId());
                });

        return summary;
    }

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

        public String getMediaTypeBreakdown() {
            StringBuilder sb = new StringBuilder();

            var books = overdueItems.stream()
                    .filter(item -> "BOOK".equals(item.getMediaType()))
                    .toList();

            var cds = overdueItems.stream()
                    .filter(item -> "CD".equals(item.getMediaType()))
                    .toList();

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
                sb.append("\n\n📦 OVERDUE ITEMS:");
                sb.append("\n").append("-".repeat(100));

                for (OverdueItem item : overdueItems) {
                    sb.append("\n").append(item);
                }

                sb.append("\n").append("-".repeat(100));
                sb.append(getMediaTypeBreakdown());
            }
            sb.append("\n").append("=".repeat(100));
            return sb.toString();
        }

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
