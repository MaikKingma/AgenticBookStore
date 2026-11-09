-- The pending restock of a stock item: the restock the distributor confirmed and no delivery has completed yet.
-- All four columns are empty when there is none; the domain checks the values, so there are no defaults or checks.

alter table stock_item
    add column pending_restock_order_id               varchar(64),
    add column pending_restock_quantity               integer,
    add column pending_restock_expected_delivery_date date,
    add column pending_restock_backordered            boolean;
