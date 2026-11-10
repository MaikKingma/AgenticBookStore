-- Orders: one row per order, and its lines in their order with snapshots of the book at the time of ordering.

create table customer_order (
    id             uuid         not null,
    customer_email varchar(254) not null,
    status         varchar(16)  not null,
    placed_at      timestamptz  not null,
    shipped_at     timestamptz,
    version        bigint       not null,
    constraint pk_customer_order primary key (id)
);

create index ix_customer_order_customer_email on customer_order (customer_email, placed_at desc, id desc);

create table order_line (
    order_id            uuid          not null,
    position            int           not null,
    book_id             uuid          not null,
    isbn                varchar(13)   not null,
    title               varchar(200)  not null,
    unit_price_amount   numeric(10,2) not null,
    unit_price_currency varchar(3)    not null,
    quantity            int           not null,
    constraint pk_order_line primary key (order_id, position),
    constraint uq_order_line_book unique (order_id, book_id),
    constraint fk_order_line_customer_order foreign key (order_id) references customer_order (id),
    constraint fk_order_line_book foreign key (book_id) references book (id)
);
