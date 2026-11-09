-- Demo data for local runs of the bookstore: forty books with their stock and six orders.
--
-- This repeatable migration lives in db/seed, which only the default profile loads (spring.flyway.locations in
-- application.yml). The test profile loads db/migration only, so tests always start with an empty catalogue.
--
-- Every statement is idempotent: INSERT ... ON CONFLICT DO NOTHING. Running the script again changes no row, and
-- any row you changed in the demo data by hand stays as it is. A row you deleted comes back, because the script
-- inserts it again. The ids are fixed literals so that a rerun finds the rows it wrote the first time.
--
-- The first twenty books are the ones the distributor also lists, with its titles and authors. The other twenty are
-- invented, with ISBNs from the 978990003 range, which the distributor does not use.
-- Customers, orders and stock figures are invented too. Sense and Sensibility (9789900010029) is left out on purpose:
-- http/inventory.http adds it.
--
-- Stock figures: seven books are at or below their reorder level, three of them sold out. Those seven carry
-- low_stock_signalled = true because the running-low event has already fired for them. Every other item is above its
-- level with the signal false. Treasure Island sits five copies above its level of three, so one order for six
-- copies makes it run low and triggers an automatic restock request.
-- No restock is pending. reserved always equals the quantity in orders that are still PLACED.

INSERT INTO book (id, isbn, title, genre, price_amount, price_currency, publication_year, status, version)
VALUES
    ('0b00c000-0000-4000-8000-000000000001', '9780141439518', 'Pride and Prejudice', 'FICTION', 9.99, 'EUR', 1813, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000002', '9789900010012', 'Emma', 'ROMANCE', 8.99, 'EUR', 1815, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000003', '9789900010043', 'Frankenstein', 'FICTION', 7.99, 'EUR', 1818, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000004', '9789900010067', 'The Time Machine', 'SCIENCE_FICTION', 6.99, 'EUR', 1895, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000005', '9789900010074', 'The War of the Worlds', 'SCIENCE_FICTION', 7.99, 'EUR', 1898, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000006', '9789900010159', 'A Study in Scarlet', 'MYSTERY', 6.99, 'EUR', 1887, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000007', '9789900010296', 'Jane Eyre', 'ROMANCE', 8.99, 'EUR', 1847, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000008', '9789900010302', 'Wuthering Heights', 'ROMANCE', 8.99, 'EUR', 1847, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000009', '9789900010715', 'Treasure Island', 'FICTION', 7.99, 'EUR', 1883, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000010', '9789900010746', 'Dracula', 'FICTION', 8.99, 'EUR', 1897, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000011', '9789900010753', 'The Picture of Dorian Gray', 'FICTION', 7.99, 'EUR', 1891, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000012', '9789900010760', 'Leaves of Grass', 'POETRY', 8.99, 'EUR', 1855, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000013', '9789900010777', 'The Adventures of Tom Sawyer', 'FICTION', 7.99, 'EUR', 1876, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000014', '9789900010807', 'Moby-Dick', 'FICTION', 10.99, 'EUR', 1851, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000015', '9789900010821', 'Little Women', 'CHILDREN', 8.99, 'EUR', 1868, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000016', '9789900010913', 'The Moonstone', 'MYSTERY', 8.99, 'EUR', 1868, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000017', '9789900011040', 'Phantastes', 'FANTASY', 8.99, 'EUR', 1858, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000018', '9789900011149', 'The History of the Decline and Fall of the Roman Empire', 'HISTORY', 49.99, 'EUR', 1776, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000019', '9789900011163', 'On the Origin of Species', 'SCIENCE', 11.99, 'EUR', 1859, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000020', '9789900011187', 'Passages from the Life of a Philosopher', 'TECHNOLOGY', 12.99, 'EUR', 1864, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000021', '9789900030010', 'The Lantern Keeper of Harrow Point', 'FICTION', 14.50, 'EUR', 2019, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000022', '9789900030027', 'Salt and Cedar', 'FICTION', 12.95, 'EUR', 2021, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000023', '9789900030034', 'Letters from the Orchard Hotel', 'ROMANCE', 11.50, 'EUR', 2018, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000024', '9789900030041', 'A Summer in Valdemar', 'ROMANCE', 10.95, 'EUR', 2022, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000025', '9789900030058', 'The Glass Orbit', 'SCIENCE_FICTION', 15.99, 'EUR', 2020, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000026', '9789900030065', 'Signal from Kepler Station', 'SCIENCE_FICTION', 16.99, 'EUR', 2023, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000027', '9789900030072', 'Ten Thousand Tiny Suns', 'SCIENCE_FICTION', 13.50, 'EUR', 2017, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000028', '9789900030089', 'Daughter of the Heron King', 'FANTASY', 14.99, 'EUR', 2016, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000029', '9789900030096', 'Ashes of the Ninth Realm', 'FANTASY', 17.50, 'EUR', 2021, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000030', '9789900030102', 'The Cartographer of Lost Rivers', 'FANTASY', 15.50, 'EUR', 2019, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000031', '9789900030119', 'Death at the Ferry Landing', 'MYSTERY', 9.95, 'EUR', 2015, 'DISCONTINUED', 0),
    ('0b00c000-0000-4000-8000-000000000032', '9789900030126', 'Alibi at the Clock Tower', 'MYSTERY', 11.95, 'EUR', 2020, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000033', '9789900030133', 'Harbour Towns of the North Sea', 'HISTORY', 24.95, 'EUR', 2014, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000034', '9789900030140', 'The Quiet Revolution of 1848', 'HISTORY', 27.50, 'EUR', 2012, 'DISCONTINUED', 0),
    ('0b00c000-0000-4000-8000-000000000035', '9789900030157', 'How Rivers Shape Cities', 'SCIENCE', 29.95, 'EUR', 2018, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000036', '9789900030164', 'The Secret Life of Lichens', 'SCIENCE', 22.00, 'EUR', 2016, 'DISCONTINUED', 0),
    ('0b00c000-0000-4000-8000-000000000037', '9789900030171', 'Practical Event Sourcing', 'TECHNOLOGY', 44.95, 'EUR', 2022, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000038', '9789900030188', 'Pipelines Without Panic', 'TECHNOLOGY', 39.95, 'EUR', 2024, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000039', '9789900030195', 'Bram the Brave Hedgehog', 'CHILDREN', 8.95, 'EUR', 2020, 'AVAILABLE', 0),
    ('0b00c000-0000-4000-8000-000000000040', '9789900030201', 'Poems for the Early Tram', 'POETRY', 13.95, 'EUR', 2019, 'AVAILABLE', 0)
ON CONFLICT DO NOTHING;

INSERT INTO book_author (book_id, position, name)
VALUES
    ('0b00c000-0000-4000-8000-000000000001', 0, 'Jane Austen'),
    ('0b00c000-0000-4000-8000-000000000002', 0, 'Jane Austen'),
    ('0b00c000-0000-4000-8000-000000000003', 0, 'Mary Shelley'),
    ('0b00c000-0000-4000-8000-000000000004', 0, 'H. G. Wells'),
    ('0b00c000-0000-4000-8000-000000000005', 0, 'H. G. Wells'),
    ('0b00c000-0000-4000-8000-000000000006', 0, 'Arthur Conan Doyle'),
    ('0b00c000-0000-4000-8000-000000000007', 0, 'Charlotte Bronte'),
    ('0b00c000-0000-4000-8000-000000000008', 0, 'Emily Bronte'),
    ('0b00c000-0000-4000-8000-000000000009', 0, 'Robert Louis Stevenson'),
    ('0b00c000-0000-4000-8000-000000000010', 0, 'Bram Stoker'),
    ('0b00c000-0000-4000-8000-000000000011', 0, 'Oscar Wilde'),
    ('0b00c000-0000-4000-8000-000000000012', 0, 'Walt Whitman'),
    ('0b00c000-0000-4000-8000-000000000013', 0, 'Mark Twain'),
    ('0b00c000-0000-4000-8000-000000000014', 0, 'Herman Melville'),
    ('0b00c000-0000-4000-8000-000000000015', 0, 'Louisa May Alcott'),
    ('0b00c000-0000-4000-8000-000000000016', 0, 'Wilkie Collins'),
    ('0b00c000-0000-4000-8000-000000000017', 0, 'George MacDonald'),
    ('0b00c000-0000-4000-8000-000000000018', 0, 'Edward Gibbon'),
    ('0b00c000-0000-4000-8000-000000000019', 0, 'Charles Darwin'),
    ('0b00c000-0000-4000-8000-000000000020', 0, 'Charles Babbage'),
    ('0b00c000-0000-4000-8000-000000000021', 0, 'Marit Solvang'),
    ('0b00c000-0000-4000-8000-000000000022', 0, 'Odile Fontaine'),
    ('0b00c000-0000-4000-8000-000000000023', 0, 'Hannelore Vos'),
    ('0b00c000-0000-4000-8000-000000000024', 0, 'Teodor Ilves'),
    ('0b00c000-0000-4000-8000-000000000025', 0, 'Ines Calderon'),
    ('0b00c000-0000-4000-8000-000000000026', 0, 'Rurik Haldane'),
    ('0b00c000-0000-4000-8000-000000000027', 0, 'Priya Nandakumar'),
    ('0b00c000-0000-4000-8000-000000000028', 0, 'Brannoch Teague'),
    ('0b00c000-0000-4000-8000-000000000029', 0, 'Wilhelmina Ashgrove'),
    ('0b00c000-0000-4000-8000-000000000030', 0, 'Soren Tidewell'),
    ('0b00c000-0000-4000-8000-000000000031', 0, 'Agnes Pellworm'),
    ('0b00c000-0000-4000-8000-000000000032', 0, 'Lucien Marchetti'),
    ('0b00c000-0000-4000-8000-000000000032', 1, 'Noor Bakker'),
    ('0b00c000-0000-4000-8000-000000000033', 0, 'Femke de Waal'),
    ('0b00c000-0000-4000-8000-000000000034', 0, 'Anselm Reuter'),
    ('0b00c000-0000-4000-8000-000000000035', 0, 'Imogen Hartley'),
    ('0b00c000-0000-4000-8000-000000000036', 0, 'Tobias Grunewald'),
    ('0b00c000-0000-4000-8000-000000000037', 0, 'Lena Brouwer'),
    ('0b00c000-0000-4000-8000-000000000037', 1, 'Jonas Meijer'),
    ('0b00c000-0000-4000-8000-000000000038', 0, 'Adebayo Okafor'),
    ('0b00c000-0000-4000-8000-000000000039', 0, 'Mirjam Koster'),
    ('0b00c000-0000-4000-8000-000000000040', 0, 'Sanne Willemsen')
ON CONFLICT DO NOTHING;

INSERT INTO stock_item (book_id, on_hand, reserved, reorder_level, reorder_quantity, low_stock_signalled, version)
VALUES
    ('0b00c000-0000-4000-8000-000000000001', 14, 2, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000002', 9, 0, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000003', 0, 0, 3, 10, true, 0),
    ('0b00c000-0000-4000-8000-000000000004', 12, 3, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000005', 2, 0, 3, 10, true, 0),
    ('0b00c000-0000-4000-8000-000000000006', 6, 1, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000007', 0, 0, 4, 12, true, 0),
    ('0b00c000-0000-4000-8000-000000000008', 3, 0, 3, 10, true, 0),
    ('0b00c000-0000-4000-8000-000000000009', 8, 0, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000010', 20, 4, 5, 15, false, 0),
    ('0b00c000-0000-4000-8000-000000000011', 1, 0, 3, 10, true, 0),
    ('0b00c000-0000-4000-8000-000000000012', 7, 0, 2, 8, false, 0),
    ('0b00c000-0000-4000-8000-000000000013', 0, 0, 3, 10, true, 0),
    ('0b00c000-0000-4000-8000-000000000014', 11, 1, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000015', 15, 2, 4, 12, false, 0),
    ('0b00c000-0000-4000-8000-000000000016', 4, 1, 3, 10, true, 0),
    ('0b00c000-0000-4000-8000-000000000017', 6, 0, 2, 6, false, 0),
    ('0b00c000-0000-4000-8000-000000000018', 5, 1, 2, 5, false, 0),
    ('0b00c000-0000-4000-8000-000000000019', 9, 0, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000020', 4, 0, 2, 6, false, 0),
    ('0b00c000-0000-4000-8000-000000000021', 8, 1, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000022', 10, 0, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000023', 6, 0, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000024', 5, 0, 2, 8, false, 0),
    ('0b00c000-0000-4000-8000-000000000025', 7, 1, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000026', 9, 0, 4, 12, false, 0),
    ('0b00c000-0000-4000-8000-000000000027', 4, 0, 2, 6, false, 0),
    ('0b00c000-0000-4000-8000-000000000028', 12, 0, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000029', 6, 0, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000030', 5, 0, 2, 6, false, 0),
    ('0b00c000-0000-4000-8000-000000000031', 4, 0, 2, 5, false, 0),
    ('0b00c000-0000-4000-8000-000000000032', 8, 0, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000033', 7, 0, 2, 5, false, 0),
    ('0b00c000-0000-4000-8000-000000000034', 3, 0, 1, 5, false, 0),
    ('0b00c000-0000-4000-8000-000000000035', 6, 0, 2, 5, false, 0),
    ('0b00c000-0000-4000-8000-000000000036', 2, 0, 1, 5, false, 0),
    ('0b00c000-0000-4000-8000-000000000037', 10, 1, 3, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000038', 12, 0, 4, 10, false, 0),
    ('0b00c000-0000-4000-8000-000000000039', 20, 2, 5, 15, false, 0),
    ('0b00c000-0000-4000-8000-000000000040', 9, 0, 3, 10, false, 0)
ON CONFLICT DO NOTHING;

INSERT INTO customer_order (id, customer_email, status, placed_at, shipped_at, version)
VALUES
    ('0d0e0000-0000-4000-8000-000000000001', 'reader@example.com', 'SHIPPED', '2026-10-20T09:00:00Z', '2026-10-22T14:00:00Z', 0),
    ('0d0e0000-0000-4000-8000-000000000002', 'bookworm@example.org', 'SHIPPED', '2026-10-12T16:20:00Z', '2026-10-14T08:30:00Z', 0),
    ('0d0e0000-0000-4000-8000-000000000003', 'reader@example.com', 'PLACED', '2026-11-03T10:15:00Z', NULL, 0),
    ('0d0e0000-0000-4000-8000-000000000004', 'bookworm@example.org', 'PLACED', '2026-11-05T18:40:00Z', NULL, 0),
    ('0d0e0000-0000-4000-8000-000000000005', 'night.owl@example.net', 'PLACED', '2026-11-06T22:05:00Z', NULL, 0),
    ('0d0e0000-0000-4000-8000-000000000006', 'night.owl@example.net', 'PLACED', '2026-11-08T21:30:00Z', NULL, 0)
ON CONFLICT DO NOTHING;

INSERT INTO order_line (order_id, position, book_id, isbn, title, unit_price_amount, unit_price_currency, quantity)
VALUES
    ('0d0e0000-0000-4000-8000-000000000001', 0, '0b00c000-0000-4000-8000-000000000002', '9789900010012', 'Emma', 8.99, 'EUR', 1),
    ('0d0e0000-0000-4000-8000-000000000001', 1, '0b00c000-0000-4000-8000-000000000007', '9789900010296', 'Jane Eyre', 8.99, 'EUR', 1),
    ('0d0e0000-0000-4000-8000-000000000001', 2, '0b00c000-0000-4000-8000-000000000023', '9789900030034', 'Letters from the Orchard Hotel', 11.50, 'EUR', 1),
    ('0d0e0000-0000-4000-8000-000000000002', 0, '0b00c000-0000-4000-8000-000000000013', '9789900010777', 'The Adventures of Tom Sawyer', 7.99, 'EUR', 2),
    ('0d0e0000-0000-4000-8000-000000000002', 1, '0b00c000-0000-4000-8000-000000000019', '9789900011163', 'On the Origin of Species', 11.99, 'EUR', 1),
    ('0d0e0000-0000-4000-8000-000000000002', 2, '0b00c000-0000-4000-8000-000000000027', '9789900030072', 'Ten Thousand Tiny Suns', 13.50, 'EUR', 1),
    ('0d0e0000-0000-4000-8000-000000000003', 0, '0b00c000-0000-4000-8000-000000000001', '9780141439518', 'Pride and Prejudice', 9.99, 'EUR', 2),
    ('0d0e0000-0000-4000-8000-000000000003', 1, '0b00c000-0000-4000-8000-000000000010', '9789900010746', 'Dracula', 8.99, 'EUR', 1),
    ('0d0e0000-0000-4000-8000-000000000003', 2, '0b00c000-0000-4000-8000-000000000021', '9789900030010', 'The Lantern Keeper of Harrow Point', 14.50, 'EUR', 1),
    ('0d0e0000-0000-4000-8000-000000000004', 0, '0b00c000-0000-4000-8000-000000000010', '9789900010746', 'Dracula', 8.99, 'EUR', 3),
    ('0d0e0000-0000-4000-8000-000000000004', 1, '0b00c000-0000-4000-8000-000000000015', '9789900010821', 'Little Women', 8.99, 'EUR', 2),
    ('0d0e0000-0000-4000-8000-000000000004', 2, '0b00c000-0000-4000-8000-000000000014', '9789900010807', 'Moby-Dick', 10.99, 'EUR', 1),
    ('0d0e0000-0000-4000-8000-000000000004', 3, '0b00c000-0000-4000-8000-000000000025', '9789900030058', 'The Glass Orbit', 15.99, 'EUR', 1),
    ('0d0e0000-0000-4000-8000-000000000005', 0, '0b00c000-0000-4000-8000-000000000004', '9789900010067', 'The Time Machine', 6.99, 'EUR', 3),
    ('0d0e0000-0000-4000-8000-000000000005', 1, '0b00c000-0000-4000-8000-000000000016', '9789900010913', 'The Moonstone', 8.99, 'EUR', 1),
    ('0d0e0000-0000-4000-8000-000000000005', 2, '0b00c000-0000-4000-8000-000000000039', '9789900030195', 'Bram the Brave Hedgehog', 8.95, 'EUR', 2),
    ('0d0e0000-0000-4000-8000-000000000006', 0, '0b00c000-0000-4000-8000-000000000006', '9789900010159', 'A Study in Scarlet', 6.99, 'EUR', 1),
    ('0d0e0000-0000-4000-8000-000000000006', 1, '0b00c000-0000-4000-8000-000000000018', '9789900011149', 'The History of the Decline and Fall of the Roman Empire', 49.99, 'EUR', 1),
    ('0d0e0000-0000-4000-8000-000000000006', 2, '0b00c000-0000-4000-8000-000000000037', '9789900030171', 'Practical Event Sourcing', 44.95, 'EUR', 1)
ON CONFLICT DO NOTHING;
