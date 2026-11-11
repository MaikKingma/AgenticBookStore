-- The cancelled time of an order: empty until the order is cancelled, like the shipped time.

alter table customer_order
    add column cancelled_at timestamptz;
