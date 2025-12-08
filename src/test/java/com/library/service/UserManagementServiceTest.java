package com.library.service;

import com.library.model.Fine;
import com.library.model.Loan;
import com.library.model.User;
import com.library.repository.FineRepository;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for UserManagementService
 * @author Library Team
 * @version 2.0
 */
class UserManagementServiceTest {
    private UserManagementService userManagementService;
    private AuthService authService;
    private UserRepository userRepository;
    private LoanRepository loanRepository;
    private FineRepository fineRepository;

    private final String TEST_USER_ID = "TEST001";
    private final String TEST_USER_ID_2 = "TEST002";
    private final String TEST_USER_ID_3 = "TEST003";

    @BeforeEach
    void setUp() {
        // Create fresh repositories
        userRepository = new UserRepository();
        loanRepository = new LoanRepository();
        fineRepository = new FineRepository();

        userManagementService = new UserManagementService(userRepository, loanRepository, fineRepository);
        authService = new AuthService();

        // Clean up any existing test users
        cleanupTestUsers();

        // Create test users
        createTestUser(TEST_USER_ID, "Test User 1", "test1@email.com", true, true);
        createTestUser(TEST_USER_ID_2, "Test User 2", "test2@email.com", true, true);
        createTestUser(TEST_USER_ID_3, "Test User 3", "test3@email.com", false, false); // Inactive user
    }

    private void cleanupTestUsers() {
        List<User> users = userRepository.getAllUsers();
        List<User> toRemove = new ArrayList<>();

        for (User user : users) {
            if (user.getUserId().startsWith("TEST")) {
                toRemove.add(user);
            }
        }

        // Remove test users using reflection
        try {
            Field usersField = UserRepository.class.getDeclaredField("users");
            usersField.setAccessible(true);
            List<User> userList = (List<User>) usersField.get(userRepository);
            userList.removeAll(toRemove);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTestUser(String userId, String name, String email, boolean isActive, boolean canBorrow) {
        User user = new User(userId, name, email);
        user.setActive(isActive);
        user.setCanBorrow(canBorrow);

        try {
            Field usersField = UserRepository.class.getDeclaredField("users");
            usersField.setAccessible(true);
            List<User> userList = (List<User>) usersField.get(userRepository);
            userList.add(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testUnregisterUserWithoutAdmin() {
        // Not logged in as admin
        UserManagementService.UnregistrationResult result =
                userManagementService.unregisterUser(TEST_USER_ID, authService);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Admin login required"));
    }

    @Test
    void testUnregisterUserWithAdminSuccess() {
        authService.login("admin", "admin123");

        // First, ensure test user is active and has no loans/fines
        User user = userRepository.findUserById(TEST_USER_ID);
        assertNotNull(user);
        assertTrue(user.isActive());

        UserManagementService.UnregistrationResult result =
                userManagementService.unregisterUser(TEST_USER_ID, authService);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("successfully unregistered"));

        // Verify user is now inactive
        User updatedUser = userRepository.findUserById(TEST_USER_ID);
        assertFalse(updatedUser.isActive());
        assertFalse(updatedUser.canBorrow());
    }

    @Test
    void testUnregisterUserAlreadyInactive() {
        authService.login("admin", "admin123");

        // Test with already inactive user
        UserManagementService.UnregistrationResult result =
                userManagementService.unregisterUser(TEST_USER_ID_3, authService);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("already inactive"));
    }

    @Test
    void testUnregisterUserWithActiveLoans() {
        authService.login("admin", "admin123");

        // Create an active loan for the user
        Loan loan = new Loan("TEST-LOAN-1", TEST_USER_ID_2, "978-0743273565", "BOOK",
                LocalDate.now(), LocalDate.now().plusDays(28));

        try {
            Field loansField = LoanRepository.class.getDeclaredField("loans");
            loansField.setAccessible(true);
            List<Loan> loanList = (List<Loan>) loansField.get(loanRepository);
            loanList.add(loan);
        } catch (Exception e) {
            e.printStackTrace();
        }

        UserManagementService.UnregistrationResult result =
                userManagementService.unregisterUser(TEST_USER_ID_2, authService);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("active loans"));
    }

    @Test
    void testUnregisterUserWithUnpaidFines() {
        authService.login("admin", "admin123");

        // Create an unpaid fine for the user
        Fine fine = new Fine("TEST-FINE-1", TEST_USER_ID, 50.0);

        try {
            Field finesField = FineRepository.class.getDeclaredField("fines");
            finesField.setAccessible(true);
            List<Fine> fineList = (List<Fine>) finesField.get(fineRepository);
            fineList.add(fine);
        } catch (Exception e) {
            e.printStackTrace();
        }

        UserManagementService.UnregistrationResult result =
                userManagementService.unregisterUser(TEST_USER_ID, authService);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("unpaid fines"));
        assertTrue(result.getMessage().contains("$50.00"));
    }

    @Test
    void testUnregisterNonExistentUser() {
        authService.login("admin", "admin123");

        UserManagementService.UnregistrationResult result =
                userManagementService.unregisterUser("NONEXISTENT", authService);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("User not found"));
    }

    @Test
    void testGetActiveUsers() {
        var activeUsers = userManagementService.getActiveUsers();

        assertFalse(activeUsers.isEmpty());
        // TEST001 and TEST002 should be active
        assertEquals(2, activeUsers.stream()
                .filter(u -> u.getUserId().startsWith("TEST"))
                .count());

        for (User user : activeUsers) {
            assertTrue(user.isActive());
        }
    }

    @Test
    void testGetInactiveUsers() {
        var inactiveUsers = userManagementService.getInactiveUsers();

        // TEST003 should be inactive
        assertEquals(1, inactiveUsers.stream()
                .filter(u -> u.getUserId().startsWith("TEST"))
                .count());

        for (User user : inactiveUsers) {
            assertFalse(user.isActive());
        }
    }

    @Test
    void testReactivateUserWithoutAdmin() {
        boolean result = userManagementService.reactivateUser(TEST_USER_ID_3, authService);
        assertFalse(result);
    }

    @Test
    void testReactivateUserSuccess() {
        authService.login("admin", "admin123");

        boolean result = userManagementService.reactivateUser(TEST_USER_ID_3, authService);
        assertTrue(result);

        // Verify user is now active
        User user = userRepository.findUserById(TEST_USER_ID_3);
        assertTrue(user.isActive());
    }

    @Test
    void testReactivateUserAlreadyActive() {
        authService.login("admin", "admin123");

        boolean result = userManagementService.reactivateUser(TEST_USER_ID, authService);
        assertFalse(result);
    }

    @Test
    void testReactivateUserWithUnpaidFines() {
        authService.login("admin", "admin123");

        // Create an unpaid fine for inactive user
        Fine fine = new Fine("TEST-FINE-2", TEST_USER_ID_3, 25.0);

        try {
            Field finesField = FineRepository.class.getDeclaredField("fines");
            finesField.setAccessible(true);
            List<Fine> fineList = (List<Fine>) finesField.get(fineRepository);
            fineList.add(fine);
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean result = userManagementService.reactivateUser(TEST_USER_ID_3, authService);
        assertTrue(result);

        // Verify user is active but cannot borrow
        User user = userRepository.findUserById(TEST_USER_ID_3);
        assertTrue(user.isActive());
        assertFalse(user.canBorrow());
    }

    @Test
    void testReactivateUserWithoutCheckingFines() {
        authService.login("admin", "admin123");

        // Create an unpaid fine for inactive user
        Fine fine = new Fine("TEST-FINE-3", TEST_USER_ID_3, 30.0);

        try {
            Field finesField = FineRepository.class.getDeclaredField("fines");
            finesField.setAccessible(true);
            List<Fine> fineList = (List<Fine>) finesField.get(fineRepository);
            fineList.add(fine);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Reactivate without checking fines
        boolean result = userManagementService.reactivateUser(TEST_USER_ID_3, authService, false);
        assertTrue(result);

        // Verify user is active and can borrow (since fines weren't checked)
        User user = userRepository.findUserById(TEST_USER_ID_3);
        assertTrue(user.isActive());
        assertTrue(user.canBorrow());
    }

    @Test
    void testReactivateNonExistentUser() {
        authService.login("admin", "admin123");

        boolean result = userManagementService.reactivateUser("NONEXISTENT", authService);
        assertFalse(result);
    }

    @Test
    void testCanUserBeUnregisteredSuccess() {
        UserManagementService.ValidationResult result =
                userManagementService.canUserBeUnregistered(TEST_USER_ID);

        assertTrue(result.isValid());
        assertTrue(result.getMessage().contains("can be unregistered"));
    }

    @Test
    void testCanUserBeUnregisteredAlreadyInactive() {
        UserManagementService.ValidationResult result =
                userManagementService.canUserBeUnregistered(TEST_USER_ID_3);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("already inactive"));
    }

    @Test
    void testCanUserBeUnregisteredWithActiveLoans() {
        // Create an active loan for the user
        Loan loan = new Loan("TEST-LOAN-2", TEST_USER_ID_2, "978-0743273565", "BOOK",
                LocalDate.now(), LocalDate.now().plusDays(28));

        try {
            Field loansField = LoanRepository.class.getDeclaredField("loans");
            loansField.setAccessible(true);
            List<Loan> loanList = (List<Loan>) loansField.get(loanRepository);
            loanList.add(loan);
        } catch (Exception e) {
            e.printStackTrace();
        }

        UserManagementService.ValidationResult result =
                userManagementService.canUserBeUnregistered(TEST_USER_ID_2);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("active loans"));
    }

    @Test
    void testCanUserBeUnregisteredWithUnpaidFines() {
        // Create an unpaid fine for the user
        Fine fine = new Fine("TEST-FINE-4", TEST_USER_ID, 75.0);

        try {
            Field finesField = FineRepository.class.getDeclaredField("fines");
            finesField.setAccessible(true);
            List<Fine> fineList = (List<Fine>) finesField.get(fineRepository);
            fineList.add(fine);
        } catch (Exception e) {
            e.printStackTrace();
        }

        UserManagementService.ValidationResult result =
                userManagementService.canUserBeUnregistered(TEST_USER_ID);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("unpaid fines"));
        assertTrue(result.getMessage().contains("$75.00"));
    }

    @Test
    void testCanUserBeUnregisteredNonExistent() {
        UserManagementService.ValidationResult result =
                userManagementService.canUserBeUnregistered("NONEXISTENT");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("User not found"));
    }

    @Test
    void testUnregistrationResultToString() {
        UserManagementService.UnregistrationResult result =
                new UserManagementService.UnregistrationResult(true, "Success");

        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("Success: Yes"));
        assertTrue(str.contains("Success"));

        result.setSuccess(false);
        result.setMessage("Failed");
        str = result.toString();
        assertTrue(str.contains("Success: No"));
        assertTrue(str.contains("Failed"));
    }

    @Test
    void testValidationResultToString() {
        UserManagementService.ValidationResult result =
                new UserManagementService.ValidationResult(true, "Valid");

        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("Valid: Yes"));
        assertTrue(str.contains("Valid"));

        result.setValid(false);
        result.setMessage("Invalid");
        str = result.toString();
        assertTrue(str.contains("Valid: No"));
        assertTrue(str.contains("Invalid"));
    }

    @Test
    void testGetters() {
        assertNotNull(userManagementService.getUserRepository());
        assertNotNull(userManagementService.getLoanRepository());
        assertNotNull(userManagementService.getFineRepository());
    }

    @Test
    void testUnregisterUserUpdateFailure() throws Exception {
        authService.login("admin", "admin123");

        // Mock the userRepository to simulate update failure
        UserRepository mockUserRepository = mock(UserRepository.class);
        User mockUser = mock(User.class);

        when(mockUserRepository.findUserById(TEST_USER_ID)).thenReturn(mockUser);
        when(mockUser.isActive()).thenReturn(true);
        when(mockUserRepository.updateUser(mockUser)).thenReturn(false);

        // Create LoanRepository and FineRepository that return no loans/fines
        LoanRepository mockLoanRepository = mock(LoanRepository.class);
        when(mockLoanRepository.getActiveLoans()).thenReturn(new ArrayList<>());

        FineRepository mockFineRepository = mock(FineRepository.class);
        when(mockFineRepository.getUnpaidFinesByUser(TEST_USER_ID)).thenReturn(new ArrayList<>());

        UserManagementService serviceWithMocks = new UserManagementService(
                mockUserRepository, mockLoanRepository, mockFineRepository);

        UserManagementService.UnregistrationResult result =
                serviceWithMocks.unregisterUser(TEST_USER_ID, authService);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Failed to update user record"));
    }

    @Test
    void testReactivateUserUpdateFailure() {
        authService.login("admin", "admin123");

        // Create a mock repository that returns a user but fails to update
        User mockUser = new User("FAILURE_USER", "Failure Test", "failure@test.com");
        mockUser.setActive(false);

        UserRepository failingRepository = new UserRepository() {
            @Override
            public boolean updateUser(User updatedUser) {
                return false; // Always fail
            }

            @Override
            public User findUserById(String userId) {
                if ("FAILURE_USER".equals(userId)) {
                    return mockUser;
                }
                return null;
            }
        };

        UserManagementService failingService = new UserManagementService(
                failingRepository, loanRepository, fineRepository);

        boolean result = failingService.reactivateUser("FAILURE_USER", authService);
        assertFalse(result);
    }
}