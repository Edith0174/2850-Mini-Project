# Library

A CSV-based Ktor web application adapted from the supermarket project for the COMP2850 library miniproject.

## What changed
- Removed employee features
- Removed database and Flyway usage
- Removed tests from the delivered zip
- Switched to CSV storage only
- Reworked the domain from supermarket products to library books

## Implemented features
- Search books by title, author, ISBN, location, or notes
- View book details, format, shelf location, and copy availability
- Sign up and log in for library users
- Borrow available copies
- Return currently borrowed copies
- Reserve unavailable books
- View active loans and reservations in the account page
- Staff-only inventory page to add, update, or remove books

## Default roles
- New sign-ups are created as `member`
- To create a staff account, sign up normally and then edit `src/main/resources/data/users.csv` to change the role to `staff`

## Run
```bash
./gradlew run
```

Then open `http://localhost:8080`.

## Data files
- `src/main/resources/data/books.csv`
- `src/main/resources/data/users.csv`
- `src/main/resources/data/loans.csv`
- `src/main/resources/data/reservations.csv`

Notes:
- The provided book list contains duplicate titles and multiple physical copies. The app treats each CSV row as one copy.
- Inventory actions modify the CSV files directly.
