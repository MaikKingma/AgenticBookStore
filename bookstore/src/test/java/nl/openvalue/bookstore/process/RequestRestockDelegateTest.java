package nl.openvalue.bookstore.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;
import nl.openvalue.bookstore.domain.StockRunningLowEvent;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The restock delegate calls exactly one inventory use case, the event-driven restock, with the book id of the event,
 * in a new transaction that may write.
 */
@ExtendWith(MockitoExtension.class)
class RequestRestockDelegateTest {

    private static final UUID BOOK_ID = UUID.fromString("3d8b5f20-6c1e-4a97-b2d4-0f7e9a1c5b63");

    @Mock
    private InventoryFlow inventory;

    @Test
    void asksTheInventoryToHandleTheRunningLowStockOfTheEventsBookAndNothingElse() {
        new RequestRestockDelegate(inventory)
                .handle(new StockRunningLowEvent(BOOK_ID, 1, 7, Instant.parse("2026-06-15T10:00:00Z")));

        verify(inventory).handleStockRunningLow(UUID.fromString("3d8b5f20-6c1e-4a97-b2d4-0f7e9a1c5b63"));
        verifyNoMoreInteractions(inventory);
    }

    @Test
    void runsInANewTransactionThatMayWrite() throws NoSuchMethodException {
        Method handle = RequestRestockDelegate.class.getDeclaredMethod("handle", StockRunningLowEvent.class);

        Transactional transactional = handle.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(transactional.readOnly()).isFalse();
        assertThat(AnnotatedElementUtils.hasAnnotation(RequestRestockDelegate.class, Transactional.class)).isFalse();
    }
}
