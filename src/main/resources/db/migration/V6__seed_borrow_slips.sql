-- Scenario coverage:
--   Slips 1-3 : returned (on time / late / early)
--   Slips 4-5 : active (not returned, within deadline)
--   Slips 6-8 : overdue (not returned, past expected_return_date)
--   Slip  9   : returned late with a lost book

insert into borrow_slips (reader_id, borrow_date, expected_return_date, actual_return_date)
values (1, '2026-01-10', '2026-01-17', '2026-01-17'),  -- slip 1 : reader 1, returned on time
       (2, '2026-02-05', '2026-02-12', '2026-02-14'),  -- slip 2 : reader 2, returned 2 days late
       (3, '2026-02-15', '2026-02-22', '2026-02-20'),  -- slip 3 : reader 3, returned early
       (5, '2026-03-29', '2026-04-05', NULL),           -- slip 4 : reader 5, active
       (6, '2026-03-30', '2026-04-06', NULL),           -- slip 5 : reader 6, active
       (7, '2026-03-10', '2026-03-17', NULL),           -- slip 6 : reader 7, overdue 18 days
       (8, '2026-03-20', '2026-03-27', NULL),           -- slip 7 : reader 8, overdue 8 days
       (9, '2026-03-22', '2026-03-29', NULL),           -- slip 8 : reader 9, overdue 6 days
       (10, '2026-02-01', '2026-02-08', '2026-02-09'); -- slip 9 : reader 10, returned 1 day late, book lost

insert into borrow_slip_books (slip_id, book_id, is_lost)
values (1, 1, false), (1, 2, false),    -- slip 1 : To Kill a Mockingbird, 1984
       (2, 3, false),                    -- slip 2 : The Great Gatsby
       (3, 4, false), (3, 5, false),    -- slip 3 : The Catcher in the Rye, The Alchemist
       (4, 7, false), (4, 8, false),    -- slip 4 : To Kill a Kingdom, Pride and Prejudice
       (5, 9, false),                    -- slip 5 : Brave New World
       (6, 10, false), (6, 11, false),  -- slip 6 : The Road, Crime and Punishment
       (7, 12, false),                   -- slip 7 : The Hobbit
       (8, 13, false), (8, 14, false),  -- slip 8 : The Shining, Fahrenheit 451
       (9, 15, true);                    -- slip 9 : Sapiens (lost)

-- Decrement quantity for books that are currently out (active/overdue) or permanently lost
-- Books 1-6 were all returned (non-lost) so their quantity is unchanged
update books set quantity = quantity - 1 where book_id in (7, 8, 9, 10, 11, 12, 13, 14, 15);
