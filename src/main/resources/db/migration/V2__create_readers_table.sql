drop table if exists readers;

create table readers (
    reader_id int auto_increment primary key,
    full_name varchar(255) not null,
    id_card_number varchar(255) not null unique,
    dob date not null,
    gender varchar(10),
    email varchar(255) unique,
    address varchar(255),
    creation_date date not null default (current_date),
    expiry_date date not null,
    created_at timestamp not null default current_timestamp
)