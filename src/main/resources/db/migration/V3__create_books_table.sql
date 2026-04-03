drop table if exists books;

create table books
(
    book_id          int auto_increment primary key,
    isbn             varchar(30)    not null unique,
    title            varchar(255)   not null,
    author           varchar(255)   not null,
    publisher        varchar(255),
    publication_year int,
    genre            varchar(100),
    price            decimal(10, 2) not null,
    quantity         int            not null,
    created_at       timestamp      not null default current_timestamp
)