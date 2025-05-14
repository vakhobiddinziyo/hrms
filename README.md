# Turnstile-Integrated Employee Management and Task Tracking System

A full-featured backend system for managing employees, controlling access via turnstiles, and tracking project-based tasks and performance metrics. Designed with scalability, role-based access control, and organizational hierarchy in mind.

## 📌 Project Objective

This project aims to:
- Automate employee attendance using integrated turnstile devices.
- Enable HR and admins to manage employee data, roles, departments, and positions.
- Provide a Kanban-style task management system with boards, states, and activity history.
- Track time spent by employees on specific tasks.

## ⚙️ Tech Stack

- **Language**: Kotlin
- **Framework**: Spring Boot
- **Database**: PostgreSQL, MongoDB
- **ORM**: Spring Data JPA, Spring Data Mongo
- **Security**: Spring Security (JWT)
- **CI/CD**: GitHub Actions / GitLab CI
- **Testing**: JUnit5, MockK

## 🧩 Key Modules

### 1. **Authentication & Authorization**
- Role-based access control (Admin, Manager, Employee, etc.)
- JWT token generation and validation

### 2. **Organization Management**
- Create and manage organizations
- Auto-generation of default head department and work schedule

### 3. **Department & Employee Management**
- Manage nested departments and positions
- Employee profile, status, and photo upload

### 4. **Turnstile Integration**
- Sync turnstile clients and devices
- Log entry/exit data with snapshot support
- Encrypt turnstile credentials using AES

### 5. **Task & Project Management**
- Project boards and Kanban-style states
- Task assignments to employees with file attachments
- Task history logs (comments, state changes, etc.)

### 6. **Working Hours Configuration**
- Define working days and required hours per day
- Validate working time with actual turnstile data

## 🧪 Test Coverage

Unit tests and integration tests are written using:
- `MockK` for mocking services and repositories
- `@SpringBootTest` and `@DataJpaTest` for integration

Run tests using:

```bash
./gradlew test
```

## 🚀 Run the Project

1. Clone the repo
2. Configure application properties:
   - PostgreSQL
   - MongoDB
   - AES encryption secret
3. Build & run:
```bash
./gradlew bootRun
```

## 📖 API Documentation

Swagger UI is available at:
```
http://localhost:9981/swagger-ui.html
```

## 🛡️ Security

- JWT-based token system
- Token refresh and expiration
- Passwords hashed via `PasswordEncoder`
- Turnstile credentials encrypted with AES

## 📂 Folder Structure (Backend)

```
src
├── config
├── controller
├── dto
├── entity
├── exception
├── repository
├── security
├── service
└── util
```

## 📈 Future Improvements

- Real-time WebSocket notifications
- Facial recognition snapshot integration
- Biometric turnstile sync
- Admin dashboard with analytics

## 👨‍💻 Author

Developed by Vakhobiddin Bobokulov.  
Feel free to fork, use, or contribute.