package com.library.observer;

import com.library.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

/**
 * Test class for Observer Pattern implementation
 * @author Library Team
 * @version 1.0
 */
class ObserverPatternTest {
    private LoanSubject subject;
    private Observer mockObserver1;
    private Observer mockObserver2;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("U001", "John Doe", "john.doe@email.com");
        subject = new LoanSubject(testUser);
        mockObserver1 = Mockito.mock(Observer.class);
        mockObserver2 = Mockito.mock(Observer.class);
    }

    @Test
    void testAttachObserver() {
        subject.attach(mockObserver1);
        // Should not throw exception
    }

    @Test
    void testDetachObserver() {
        subject.attach(mockObserver1);
        subject.detach(mockObserver1);
        // Should not throw exception
    }

    @Test
    void testNotifyObservers() {
        subject.attach(mockObserver1);
        subject.attach(mockObserver2);

        NotificationEvent event = new NotificationEvent(
                testUser,
                "TEST_EVENT",
                "Test message"
        );

        subject.notifyObservers(event);

        verify(mockObserver1, times(1)).update(event);
        verify(mockObserver2, times(1)).update(event);
    }

    @Test
    void testObserverNotNotifiedAfterDetach() {
        subject.attach(mockObserver1);
        subject.attach(mockObserver2);
        subject.detach(mockObserver2);

        NotificationEvent event = new NotificationEvent(
                testUser,
                "TEST_EVENT",
                "Test message"
        );

        subject.notifyObservers(event);

        verify(mockObserver1, times(1)).update(event);
        verify(mockObserver2, never()).update(event);
    }

    @Test
    void testMultipleEvents() {
        subject.attach(mockObserver1);

        NotificationEvent event1 = new NotificationEvent(testUser, "EVENT1", "Message1");
        NotificationEvent event2 = new NotificationEvent(testUser, "EVENT2", "Message2");

        subject.notifyObservers(event1);
        subject.notifyObservers(event2);

        verify(mockObserver1, times(1)).update(event1);
        verify(mockObserver1, times(1)).update(event2);
    }
}