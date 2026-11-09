package nl.openvalue.bookstore.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import nl.openvalue.bookstore.domain.StockRunningLowEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The event processor hands every running-low event to the restock delegate and does nothing else. It listens only
 * after the commit of the transaction that registered the event, never without a transaction, and opens no
 * transaction of its own: the delegate does that.
 */
@ExtendWith(MockitoExtension.class)
class EventProcessorTest {

    private static final StockRunningLowEvent EVENT = new StockRunningLowEvent(
            UUID.fromString("7a3e9c41-0b2d-4f86-a5c7-1e9d3b8f20a6"), 2, 13, Instant.parse("2026-06-15T10:00:00Z"));

    @Mock
    private RequestRestockDelegate delegate;

    @Test
    void handsTheRunningLowEventToTheRestockDelegateAndNothingElse() {
        new EventProcessor(delegate).onStockRunningLow(EVENT);

        verify(delegate).handle(EVENT);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    void listensToTheRunningLowEventAfterTheCommitOnly() throws NoSuchMethodException {
        Method listener = listenerMethod();

        TransactionalEventListener annotation = listener.getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(annotation.fallbackExecution()).isFalse();
        assertThat(annotation.classes()).isEmpty();
        assertThat(annotation.value()).isEmpty();
        assertThat(listener.getParameterTypes()).containsExactly(StockRunningLowEvent.class);
    }

    @Test
    void opensNoTransactionOfItsOwn() throws NoSuchMethodException {
        assertThat(AnnotatedElementUtils.hasAnnotation(EventProcessor.class, Transactional.class)).isFalse();
        assertThat(AnnotatedElementUtils.hasAnnotation(listenerMethod(), Transactional.class)).isFalse();
    }

    @Test
    void hasTheListenerAsItsOnlyMethod() throws NoSuchMethodException {
        Method listener = listenerMethod();

        assertThat(Arrays.stream(EventProcessor.class.getDeclaredMethods()).filter(method -> !method.isSynthetic()))
                .containsExactly(listener);
    }

    private static Method listenerMethod() throws NoSuchMethodException {
        return EventProcessor.class.getDeclaredMethod("onStockRunningLow", StockRunningLowEvent.class);
    }
}
