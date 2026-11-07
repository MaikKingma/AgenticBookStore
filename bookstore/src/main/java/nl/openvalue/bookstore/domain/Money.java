package nl.openvalue.bookstore.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * An exact amount of money in euro. The amount always has scale 2, rounded half even, so equal amounts are equal
 * values whatever their trailing zeros. Negative amounts after rounding are rejected.
 */
public record Money(BigDecimal amount, Currency currency) {

    private static final int SCALE = 2;
    private static final Currency EUR = Currency.getInstance("EUR");

    public Money {
        if (amount == null) {
            throw new InvalidMoneyException("An amount of money needs an amount");
        }
        if (currency == null) {
            throw new InvalidMoneyException("An amount of money needs a currency");
        }
        if (!EUR.equals(currency)) {
            throw new InvalidMoneyException(
                    "Only EUR is supported, not " + currency.getCurrencyCode());
        }
        amount = amount.setScale(SCALE, RoundingMode.HALF_EVEN);
        if (amount.signum() < 0) {
            throw new InvalidMoneyException("An amount of money cannot be negative: " + amount.toPlainString());
        }
    }

    /** Parses a decimal amount in euro, for example {@code "12.99"}. */
    public static Money of(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new InvalidMoneyException("An amount of money needs a value");
        }
        try {
            return of(new BigDecimal(amount));
        } catch (NumberFormatException e) {
            throw new InvalidMoneyException("'" + amount + "' is not a decimal amount");
        }
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, EUR);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        if (currencyCode == null) {
            throw new InvalidMoneyException("An amount of money needs a currency");
        }
        try {
            return new Money(amount, Currency.getInstance(currencyCode));
        } catch (IllegalArgumentException e) {
            throw new InvalidMoneyException("'" + currencyCode + "' is not a known currency code");
        }
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount), currency);
    }

    public Money times(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }

    public boolean isGreaterThan(Money other) {
        return amount.compareTo(other.amount) > 0;
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
