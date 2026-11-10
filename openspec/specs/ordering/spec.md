# Ordering Specification

## Purpose
How customers place orders and how placed orders are shipped: stock reservation, price snapshots and the stock reduction at shipping.

## Requirements

### Requirement: Place an order
The system SHALL place an order when every requested book exists, is available and has enough stock, reserving the stock and snapshotting prices.

#### Scenario: Successful order
- **GIVEN** book A (12.50, available 5) and book B (8.00, available 2)
- **WHEN** a customer places an order for 2 x A and 1 x B
- **THEN** the response is 202 with an order id, the order total is "33.00", A has reserved 2 and B has reserved 1, and one `OrderPlacedEvent` is published after commit

#### Scenario: Price snapshot
- **GIVEN** an order containing book A at 12.50
- **WHEN** the price of A changes to 15.00
- **THEN** the order still shows unitPrice "12.50"

#### Scenario: Several shortages are reported together
- **GIVEN** book A with available 1 and book B with available 0
- **WHEN** a customer orders 2 x A and 1 x B
- **THEN** the response is 409 with `code` `insufficient-stock` and `details` naming both books with requested and available quantities, and no stock is reserved

#### Scenario: Duplicate lines are merged
- **GIVEN** book A with available 8
- **WHEN** a customer orders book A twice with quantity 3 each
- **THEN** the order has one line for A with quantity 6 and A has reserved 6

#### Scenario: Merged quantity too large
- **WHEN** a customer orders book A twice with quantity 6 each
- **THEN** the response is 422 with `code` `quantity-out-of-range` and nothing is reserved

#### Scenario: Discontinued book
- **GIVEN** book A is discontinued
- **WHEN** a customer orders it
- **THEN** the response is 409 and nothing is reserved

### Requirement: Ship an order
The system SHALL ship a placed order, turning its reservations into stock reductions.

#### Scenario: Shipping reduces stock
- **GIVEN** a placed order for 2 x A, with A onHand 5 and reserved 2
- **WHEN** the order is shipped
- **THEN** the order status is SHIPPED, and A has onHand 3 and reserved 0

#### Scenario: Shipping twice
- **GIVEN** a shipped order
- **WHEN** it is shipped again
- **THEN** the response is 409 and stock is unchanged
