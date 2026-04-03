create table borrow_slips
(
    slip_id              int auto_increment primary key,
    reader_id            int       not null,
    borrow_date          date      not null,
    expected_return_date date      not null,
    actual_return_date   date,
    created_at           timestamp not null default current_timestamp,
    updated_at           timestamp not null default current_timestamp on
        update current_timestamp,
    foreign key (reader_id) references readers (reader_id)
);

create table borrow_slip_books
(
    slip_id int     not null,
    book_id int     not null,
    is_lost boolean not null default false,
    primary key (slip_id, book_id),
    foreign key (slip_id) references borrow_slips (slip_id),
    foreign key (book_id) references books (book_id)
);