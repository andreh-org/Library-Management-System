package com.library.observer;

import com.library.model.Loan;
import com.library.model.User;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete subject for loan-related notifications
 * Follows Observer Pattern from refactoring.guru
 * @author Library Team
 * @version 1.0
 */
public class LoanSubject implements Subject {
    private List<Observer> observers;
    private User user;
    private List<Loan> overdueLoans;

    public LoanSubject(User user) {
        this.observers = new ArrayList<>();
        this.user = user;
        this.overdueLoans = new ArrayList<>();
    }

    @Override
    public void attach(Observer observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(NotificationEvent event) {
        for (Observer observer : observers) {
            observer.update(event);
        }
    }

    /**
     * Check for overdue loans and notify observers
     * @param currentDate the current date to check against
     */
    public void checkOverdueAndNotify(LocalDate currentDate) {
        List<Loan> newlyOverdue = new ArrayList<>();

        for (Loan loan : overdueLoans) {
            if (loan.checkOverdue(currentDate) && !loan.isOverdue()) {
                newlyOverdue.add(loan);
            }
        }

        if (!newlyOverdue.isEmpty()) {
            NotificationEvent event = new NotificationEvent(
                    user,
                    "OVERDUE_DETECTED",
                    String.format("User %s has %d new overdue item(s)",
                            user.getUserId(), newlyOverdue.size()),
                    newlyOverdue
            );
            notifyObservers(event);
        }
    }

    /**
     * Add a loan to monitor for overdue status
     * @param loan the loan to monitor
     */
    public void addLoanToMonitor(Loan loan) {
        overdueLoans.add(loan);
    }

    /**
     * Remove a loan from monitoring
     * @param loan the loan to remove
     */
    public void removeLoanFromMonitor(Loan loan) {
        overdueLoans.remove(loan);
    }
}