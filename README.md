# BorrowDesk

A small campus equipment lending system for a single lab assistant: view equipment, issue an item to a student, see active loans and record returns. Built as a college DevOps mini-project (Java → Maven → GitHub → Jenkins → cloud VM).

## Features
- Dashboard with equipment and active-loan counts
- Equipment list with total and available quantity
- Issue form: student name, register number, equipment, due date
- Active loans with a **Return** button (POST), plus a loan history (latest 50)
- Friendly validation: blank details, past due date, unknown equipment, no stock left, repeated return
- `GET /health` returns `{"status":"UP"}`

## Architecture
Spring Boot 4.1.2 · Java 21 · Spring MVC + Thymeleaf · Spring Data JPA · file-based H2 · Maven Wrapper.

```
Browser ──► BorrowDeskController ──► LendingService ──► EquipmentRepository / LoanRepository ──► H2 file
              (forms, pages)         (all rules, @Transactional)
```
Two entities: `Equipment` (id, name, totalQuantity) and `Loan` (id, equipment, studentName, registerNumber, issuedDate, dueDate, returnedDate). Availability is **derived** (`totalQuantity` − active loans); no counter is stored. The equipment row is locked while issuing so the last item can't be issued twice.

Issue flow: `POST /issue` → Bean Validation on the form → `LendingService.issue` re-checks input, locks the equipment row, compares active loans with total, saves the `Loan` → redirect to `/loans` with a message. Rule failures come back as form errors, not exceptions.

## Prerequisites
- JDK 21 (`java -version`, `javac -version`) with `JAVA_HOME` pointing at it
- Git
- Internet access on first build (Maven downloads dependencies)

Maven itself is not needed; use the wrapper.

## Build, test, run
| | Windows PowerShell | Linux / WSL |
|---|---|---|
| Test + package | `.\mvnw.cmd clean verify` | `./mvnw clean verify` |
| Run (dev) | `.\mvnw.cmd spring-boot:run` | `./mvnw spring-boot:run` |
| Run JAR | `java -jar target/borrowdesk.jar` | `java -jar target/borrowdesk.jar` |

Open http://localhost:8081 (port 8081). Stop with `Ctrl+C`.

## Database
H2 file database. Directory comes from `BORROWDESK_DATA_DIR`; default is `~/.borrowdesk/data` (file `borrowdesk.mv.db`). Five sample items are seeded only when the equipment table is empty.
```powershell
$env:BORROWDESK_DATA_DIR = "D:\borrowdesk-data"   # PowerShell
```
```bash
export BORROWDESK_DATA_DIR=/var/lib/borrowdesk    # Linux
```
Tests use a separate in-memory H2 database and a fixed clock, so they never touch this file.

## Current limitations
- No authentication (single-operator demo)
- No equipment add/edit screens; seed data only
- No notifications, fines or reservations
- Embedded H2 supports one running instance per data directory
