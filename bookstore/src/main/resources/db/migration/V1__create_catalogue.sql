-- The catalogue: one row per book, and its authors in their order.

create table book (
    id               uuid          not null,
    isbn             varchar(13)   not null,
    title            varchar(200)  not null,
    genre            varchar(32)   not null,
    price_amount     numeric(10,2) not null,
    price_currency   varchar(3)    not null,
    publication_year int           not null,
    status           varchar(16)   not null,
    version          bigint        not null,
    constraint pk_book primary key (id),
    constraint uq_book_isbn unique (isbn)
);

create table book_author (
    book_id  uuid         not null,
    position int          not null,
    name     varchar(120) not null,
    constraint pk_book_author primary key (book_id, position),
    constraint fk_book_author_book foreign key (book_id) references book (id)
);
