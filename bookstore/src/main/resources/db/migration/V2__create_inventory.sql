-- The inventory: one stock item per book, with its reorder settings and the low-stock signal.

create table stock_item (
    book_id             uuid    not null,
    on_hand             integer not null,
    reserved            integer not null,
    reorder_level       integer not null,
    reorder_quantity    integer not null,
    low_stock_signalled boolean not null,
    version             bigint  not null,
    constraint pk_stock_item primary key (book_id),
    constraint fk_stock_item_book foreign key (book_id) references book (id)
);
