package nl.openvalue.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void roundsHalfEvenToTwoDecimals() {
        assertThat(Money.of("10.005").amount().toPlainString()).isEqualTo("10.00");
        assertThat(Money.of("10.015").amount().toPlainString()).isEqualTo("10.02");
    }

    @Test
    void alwaysHasScaleTwo() {
        assertThat(Money.of("12.5").amount().toPlainString()).isEqualTo("12.50");
        assertThat(Money.of("7").amount().toPlainString()).isEqualTo("7.00");
    }

    @Test
    void isEqualRegardlessOfTrailingZeros() {
        assertThat(Money.of("12.5")).isEqualTo(Money.of("12.50"));
        assertThat(Money.of(new BigDecimal("12.500"))).isEqualTo(Money.of("12.50"));
    }

    @Test
    void isInEuro() {
        assertThat(Money.of("12.99").currency()).isEqualTo(EUR);
        assertThat(Money.of(new BigDecimal("12.99")).currency()).isEqualTo(EUR);
    }

    @Test
    void acceptsEuroByCurrencyCode() {
        assertThat(Money.of(new BigDecimal("12.99"), "EUR")).isEqualTo(Money.of("12.99"));
    }

    @Test
    void acceptsZero() {
        assertThat(Money.of("0.00").amount().toPlainString()).isEqualTo("0.00");
    }

    @Test
    void acceptsANegativeAmountThatRoundsToZero() {
        assertThat(Money.of("-0.004").amount().toPlainString()).isEqualTo("0.00");
    }

    @Test
    void rejectsANegativeAmount() {
        assertRejected(() -> Money.of("-0.01"));
        assertRejected(() -> Money.of(new BigDecimal("-12.99")));
    }

    @Test
    void rejectsACurrencyOtherThanEuro() {
        assertRejected(() -> Money.of(new BigDecimal("12.99"), "USD"));
        assertRejected(() -> new Money(new BigDecimal("12.99"), Currency.getInstance("GBP")));
    }

    @Test
    void rejectsAnUnknownOrMissingCurrencyCode() {
        assertRejected(() -> Money.of(new BigDecimal("12.99"), "XYZ"));
        assertRejected(() -> Money.of(new BigDecimal("12.99"), null));
    }

    @Test
    void rejectsAMissingAmountOrCurrency() {
        assertRejected(() -> new Money(null, EUR));
        assertRejected(() -> new Money(new BigDecimal("12.99"), null));
        assertRejected(() -> Money.of((BigDecimal) null));
    }

    @Test
    void rejectsNullBlankAndUnparsableText() {
        assertRejected(() -> Money.of((String) null));
        assertRejected(() -> Money.of(""));
        assertRejected(() -> Money.of("   "));
        assertRejected(() -> Money.of("twelve"));
        assertRejected(() -> Money.of("12,99"));
    }

    @Test
    void addsTwoAmounts() {
        assertThat(Money.of("9.99").plus(Money.of("2.51"))).isEqualTo(Money.of("12.50"));
    }

    @Test
    void multipliesByAQuantity() {
        assertThat(Money.of("12.99").times(3)).isEqualTo(Money.of("38.97"));
        assertThat(Money.of("12.99").times(0)).isEqualTo(Money.of("0.00"));
    }

    @Test
    void comparesAmounts() {
        assertThat(Money.of("12.51").isGreaterThan(Money.of("12.50"))).isTrue();
        assertThat(Money.of("12.5").isGreaterThan(Money.of("12.50"))).isFalse();
        assertThat(Money.of("12.49").isGreaterThan(Money.of("12.50"))).isFalse();
    }

    @Test
    void printsTheAmountAndTheCurrencyCode() {
        assertThat(Money.of("12.99")).hasToString("12.99 EUR");
        assertThat(Money.of("12.5")).hasToString("12.50 EUR");
    }

    private static void assertRejected(ThrowingCallable creation) {
        assertThatThrownBy(creation)
                .isInstanceOf(InvalidMoneyException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }
}
