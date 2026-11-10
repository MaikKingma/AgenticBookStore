# Money Is Exact

Never use `double` or `float` for money, boxed or not.

- In `domain` a price is the record `Money(BigDecimal amount, Currency currency)`: scale 2, rounding `HALF_EVEN`, currency EUR only, never negative.
- Create it with `Money.of("12.99")` or `Money.of(BigDecimal)`.
- Across a port a price is `BigDecimal` plus a `String` currency code (`BookDTO.priceAmount`, `BookDTO.priceCurrency`).
- In a request payload or a view the amount is a plain decimal string (`AddBookPayload.price`, `BookView.price`) next to the currency.
- `NO_DOUBLE_OR_FLOAT_FOR_MONEY` (ArchUnit) covers `domain`, `domaininteraction`, `command` and `query`: no field and no method or constructor parameter of type `double` or `float`.
- Database columns: `numeric(10,2)` for the amount and `varchar(3)` for the currency (`price_amount`, `price_currency`).
