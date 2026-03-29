drop table if exists librarians;

create table librarians (
    librarian_id int not null auto_increment primary key,
    username varchar(255) not null unique,
    password_hash varchar(255) not null,
    created_at timestamp not null default current_timestamp
);