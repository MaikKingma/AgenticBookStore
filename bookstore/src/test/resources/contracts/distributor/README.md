# Distributor contract files

These files are the bodies the real distributor returned (and the request bodies sent to it), stored byte for byte as received: no pretty-printing and no edits, except the four derived files listed at the end. The bookstore contract tests replay them through a `MockRestServiceServer`, so the tests assert what the real distributor says and not what we guess it says.

How the capture was done:

- Capture date: 2026-11-08 (Sunday, Europe/Amsterdam). Every file below was captured on that day except `title-9789900010395.json`, which the title files section describes.
- Distributor for every file except `title-9789900010395.json`: started from the repository root with `./mvnw -pl distributor spring-boot:run`, listening on port 8090 on 127.0.0.1. Run 1 used no flags (defaults). Run 2 used `--distributor.chaos.failure-rate=1.0` and produced only `unavailable-503.json`.
- For every file except `title-9789900010395.json`: Requests were sent with curl, as written in `http/distributor.http` (the compact request bodies below carry the same keys and values as the pretty-printed bodies in that file). Every response body was written to its file untouched.
- Both of these distributor processes were stopped afterwards and port 8090 was free again.

Dates and ids come from the capture day. The restock order ids are random, and the expected delivery dates are the capture day plus 2 working days (CONFIRMED) or plus 14 calendar days (BACKORDERED), counted in Europe/Amsterdam. The spec example date 2026-11-17 is only an example. The bookstore tests assert the values captured here, so a recapture on another day means updating the literals below and the tests together. The same holds for the release date of 9789900020080, which the distributor derives from the capture day (16 days before it).

## Title files (GET /api/titles/{ean}, status 200, run 1 unless noted, no flags)

Every title has the currency EUR. Every title has only contributors with the role AUTHOR, except `title-9789900010395.json`, which also has a TRANSLATOR (Henry Wadsworth Longfellow) and an ILLUSTRATOR (Gustave Dore). That file was captured on 2026-11-10 with curl from a distributor on a free port: the REST body and the MCP `get_title` text were identical bytes. The first subject code is the one that decides the bookstore genre. Prices are `listPriceCents`.

| File | Source request | Title | Authors in order | Subject codes | Price (cents) | Availability | Release date |
|---|---|---|---|---|---|---|---|
| `title-9780141439518.json` | GET /api/titles/9780141439518 | Pride and Prejudice | Jane Austen | FIC004000 | 999 | IN_STOCK | 1813-01-28 |
| `title-9789900010050.json` | GET /api/titles/9789900010050 | The Last Man | Mary Shelley | FIC028000 | 1199 | IN_STOCK | 1826-01-01 |
| `title-9789900010791.json` | GET /api/titles/9789900010791 | A Connecticut Yankee in King Arthur's Court | Mark Twain | FIC009000, FIC028000 | 899 | IN_STOCK | 1889-01-01 |
| `title-9789900010159.json` | GET /api/titles/9789900010159 | A Study in Scarlet | Arthur Conan Doyle | FIC022000 | 699 | IN_STOCK | 1887-01-01 |
| `title-9789900010029.json` | GET /api/titles/9789900010029 | Sense and Sensibility | Jane Austen | FIC027000 | 899 | IN_STOCK | 1811-10-30 |
| `title-9789900010043.json` | GET /api/titles/9789900010043 | Frankenstein | Mary Shelley | FIC015000, FIC028000 | 799 | IN_STOCK | 1818-01-01 |
| `title-9789900010616.json` | GET /api/titles/9789900010616 | Utopia | Thomas More | HIS000000 | 299 | IN_STOCK | 1516-01-01 |
| `title-9789900011163.json` | GET /api/titles/9789900011163 | On the Origin of Species | Charles Darwin | SCI000000 | 1199 | IN_STOCK | 1859-11-24 |
| `title-9789900011187.json` | GET /api/titles/9789900011187 | Passages from the Life of a Philosopher | Charles Babbage | COM000000 | 1299 | IN_STOCK | 1864-01-01 |
| `title-9789900010821.json` | GET /api/titles/9789900010821 | Little Women | Louisa May Alcott | JUV000000 | 899 | IN_STOCK | 1868-01-01 |
| `title-9789900010210.json` | GET /api/titles/9789900010210 | The Raven and Other Poems | Edgar Allan Poe | POE000000 | 1499 | IN_STOCK | 1845-01-01 |
| `title-9789900010678.json` | GET /api/titles/9789900010678 | Pamela | Samuel Richardson | FIC027000 | 1099 | OUT_OF_PRINT | 1740-01-01 |
| `title-9789900010272.json` | GET /api/titles/9789900010272 | Bleak House | Charles Dickens | FIC004000 | 1299 | BACKORDER | 1853-01-01 |
| `title-9789900020080.json` | GET /api/titles/9789900020080 | Patterns for Patient Systems | Dagny Vellacourt, Emeric Stroud-Halvorsen | COM000000 | 2499 | IN_STOCK | 2026-10-23 |
| `title-9789900010395.json` | GET /api/titles/9789900010395 | The Divine Comedy: Inferno | Dante Alighieri | POE000000 | 3499 | IN_STOCK | 1867-01-01 |

Release years the tests assert: 1813, 1826, 1889, 1887, 1811, 1818, 1516, 1859, 1864, 1868, 1845, 1740, 1853, and 2026 for 9789900020080.

First subject code prefixes covered: FIC004 (Pride and Prejudice, Bleak House), FIC028 (The Last Man), FIC009 (A Connecticut Yankee, with FIC028000 second), FIC022 (A Study in Scarlet), FIC027 (Sense and Sensibility, Pamela), FIC015 (Frankenstein, with FIC028000 second), HIS (Utopia), SCI (On the Origin of Species), COM (Passages from the Life of a Philosopher, Patterns for Patient Systems), JUV (Little Women) and POE (The Raven and Other Poems, The Divine Comedy: Inferno).

Availability per title: all IN_STOCK except Pamela (OUT_OF_PRINT, so not orderable) and Bleak House (BACKORDER, still orderable). Warehouse quantities and formats are in the files and are not asserted by the bookstore.

## Error files (run 1 unless noted, no flags)

| File | Source request | Status | Body |
|---|---|---|---|
| `title-not-found-404.json` | GET /api/titles/9780000000002 (no body) | 404 | errorCode TITLE_NOT_FOUND, message "No title with EAN 9780000000002" |
| `unknown-path-404.json` | GET /api/no-such-resource (no body) | 404 | default web framework body with timestamp, status 404, error "Not Found" and path; it has no errorCode. The timestamp is the capture moment (2026-11-08T20:52:11.542Z) and is not asserted. |
| `unavailable-503.json` | GET /api/titles/9780141439518 (no body), run 2 with `--distributor.chaos.failure-rate=1.0` | 503 | errorCode TEMPORARILY_UNAVAILABLE, message "The distributor is temporarily unavailable, try again later" |

## Restock files (POST /api/restock-orders with Content-Type application/json, run 1, no flags)

Each request file (name ending in -request.json) holds the exact body that was sent (all three keys: ean, quantity, retailerReference). The matching response file (name ending in -response.json) holds the body that came back. A retailer reference is stored by the distributor and is not echoed.

| Request file | Response file | Request body | Status | Response literals |
|---|---|---|---|---|
| `restock-confirmed-request.json` | `restock-confirmed-response.json` | `{"ean":"9780141439518","quantity":10,"retailerReference":"workshop-order-1"}` | 201 | restockOrderId 7597c6da-0041-473d-861a-d4b84c7fb204, ean 9780141439518, quantity 10, status CONFIRMED, expectedDeliveryDate 2026-11-10 |
| `restock-backordered-request.json` | `restock-backordered-response.json` | `{"ean":"9789900010272","quantity":5,"retailerReference":"workshop-order-2"}` | 201 | restockOrderId 207e15be-cf0e-47da-90e8-60396068690f, ean 9789900010272, quantity 5, status BACKORDERED, expectedDeliveryDate 2026-11-22 |
| `restock-not-orderable-request.json` | `restock-not-orderable-response.json` | `{"ean":"9789900010678","quantity":5,"retailerReference":"workshop-order-3"}` | 422 | errorCode NOT_ORDERABLE, message "Title with EAN 9789900010678 is not orderable" |
| `restock-title-not-found-request.json` | `restock-title-not-found-response.json` | `{"ean":"9780000000002","quantity":5,"retailerReference":"workshop-order-4"}` | 404 | errorCode TITLE_NOT_FOUND, message "No title with EAN 9780000000002" |
| `restock-invalid-request-request.json` | `restock-invalid-request-response.json` | `{"ean":"9780141439518","quantity":0,"retailerReference":"workshop-order-5"}` | 400 | errorCode INVALID_REQUEST, message "quantity must be a whole number between 1 and 500" |

The two successful responses have exactly five keys: restockOrderId, ean, quantity, status, expectedDeliveryDate. The bookstore maps CONFIRMED to a restock that is not backordered and BACKORDERED to one that is.

## Derived files

A derived file is not a captured body. Each one is a copy of exactly one captured file with exactly one change, made so that a case the real distributor never produces can be tested. Nothing else in the copy differs.

| File | Source file | The single change |
|---|---|---|
| `derived-title-usd-currency.json` | `title-9780141439518.json` | `currency` changed from "EUR" to "USD" (the seed has no non-EUR title) |
| `derived-title-unmapped-subject.json` | `title-9789900010050.json` | `subjectCodes` changed from ["FIC028000"] to ["XYZ000000", "FIC028000"] (no seed title has an unmapped first code) |
| `derived-title-round-price.json` | `title-9780141439518.json` | `listPriceCents` changed from 999 to 1200 (a price whose euro amount ends in a zero, to check scale 2) |
| `derived-title-authors-reversed.json` | `title-9789900020080.json` | the two `contributors` entries swapped, so the order is Emeric Stroud-Halvorsen then Dagny Vellacourt (the order must follow the list, not the alphabet) |
