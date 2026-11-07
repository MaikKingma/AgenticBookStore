package nl.openvalue.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class QuantityTest {

    @Test
    void acceptsZero() {
        assertThat(new Quantity(0).value()).isEqualTo(0);
    }

    @Test
    void acceptsTenThousand() {
        assertThat(new Quantity(10000).value()).isEqualTo(10000);
    }

    @Test
    void rejectsMinusOne() {
        assertRejected(() -> new Quantity(-1));
    }

    @Test
    void rejectsTenThousandAndOne() {
        assertRejected(() -> new Quantity(10001));
    }

    private static void assertRejected(ThrowingCallable creation) {
        assertThatThrownBy(creation)
                .isInstanceOf(InvalidQuantityException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }
}
