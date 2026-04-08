# Libry.io

A RESTful Library Management API built with Spring Boot. It manages librarian accounts, reader cards, books, and borrow/return slips, with JWT-based authentication and fine calculation for overdue or lost books.

## Tech Stack

- **Java 21** / **Spring Boot 3.5.13**
- **Spring Security** + **JWT** (jjwt 0.13.0)
- **Spring Data JPA** + **MySQL 8.0**
- **Flyway** for database migrations
- **Lombok**
- **Docker Compose** for local database setup

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### 1. Start the database

```bash
docker compose up -d
```

This starts a MySQL 8.0 instance on port `3306` with the database `libry`.

### 2. Run the application

```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`. Flyway runs migrations automatically on startup.

### Environment Variables

All variables have defaults suitable for local development.

| Variable        | Default                                   | Description              |
|-----------------|-------------------------------------------|--------------------------|
| `DB_HOST`       | `localhost`                               | MySQL host               |
| `DB_PORT`       | `3306`                                    | MySQL port               |
| `DB_NAME`       | `libry`                                   | Database name            |
| `DB_USERNAME`   | `libry`                                   | Database user            |
| `DB_PASSWORD`   | `libry`                                   | Database password        |
| `SERVER_PORT`   | `8080`                                    | HTTP server port         |
| `JWT_SECRET`    | `MJc3tLH3ymP7r2YIMd0Vv640BQjChXTdGruwfuW5Lz5` | JWT signing secret  |
| `JWT_EXPIRATION`| `3600000`                                 | Token TTL in milliseconds (1 hour) |

## API Reference

All endpoints under `/api` (except `/api/auth/register` and `/api/auth/login`) require a `Bearer` token in the `Authorization` header.

### Authentication — `/api/auth`

| Method | Path              | Description               |
|--------|-------------------|---------------------------|
| POST   | `/register`       | Create a librarian account |
| POST   | `/login`          | Login and receive a JWT    |
| POST   | `/logout`         | Logout (client-side token discard) |

### Books — `/api/books`

| Method | Path              | Description                        |
|--------|-------------------|------------------------------------|
| GET    | `/`               | List all books (paginated)         |
| POST   | `/`               | Add a new book                     |
| GET    | `/{id}`           | Get a book by ID                   |
| PUT    | `/{id}`           | Replace a book's information       |
| PATCH  | `/{id}`           | Partially update a book            |
| DELETE | `/{id}`           | Delete a book                      |
| GET    | `/search?isbn=`   | Search by ISBN                     |
| GET    | `/search?title=`  | Search by title (paginated)        |

### Readers — `/api/readers`

| Method | Path                         | Description                         |
|--------|------------------------------|-------------------------------------|
| GET    | `/`                          | List all readers (paginated)        |
| POST   | `/`                          | Register a new reader               |
| GET    | `/{id}`                      | Get a reader by ID                  |
| PUT    | `/{id}`                      | Replace a reader's information      |
| PATCH  | `/{id}`                      | Partially update a reader           |
| DELETE | `/{id}`                      | Delete a reader                     |
| GET    | `/search?id_card_number=`    | Search by national ID               |
| GET    | `/search?full_name=`         | Search by name (paginated)          |

### Borrow Slips — `/api/borrow-slips`

| Method | Path              | Description                                    |
|--------|-------------------|------------------------------------------------|
| GET    | `/`               | List all borrow slips (paginated)              |
| POST   | `/`               | Create a borrow slip                           |
| GET    | `/{id}`           | Get a borrow slip by ID                        |
| PATCH  | `/{id}/return`    | Process a book return (calculates fines)       |

### Statistics — `/api/statistics`

| Method | Path               | Description                                         |
|--------|--------------------|-----------------------------------------------------|
| GET    | `/books`           | Total books and breakdown by genre                  |
| GET    | `/readers`         | Total readers and breakdown by gender               |
| GET    | `/readers/overdue` | List of readers with currently overdue borrows      |

## Business Rules

- **Reader card** is valid for **48 months** from the date of creation.
- Books may be borrowed for a maximum of **7 days**.
- **Overdue fine**: 5,000 VND per day past the due date.
- **Lost book fine**: 200% of the book's listed price.

## Running Tests

```bash
./mvnw test
```

Tests use an in-memory H2 database and do not require Docker.
