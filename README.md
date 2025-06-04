# Trekha

Trekha is a ride-sharing platform built with Spring Boot, supporting user registration, authentication, KYC document management, and role-based access control.

## Features

- User registration via email or mobile
- Role-based access (Passenger, Driver, Vehicle Owner, Admin)
- KYC document upload and status tracking
- Secure password storage (BCrypt)
- RESTful API with validation and global exception handling
- OpenAPI/Swagger documentation

## Project Structure

```
src/
  main/
    java/com/app/trekha/
      user/           # User models, DTOs, repositories, services, controllers
      storage/        # File storage service interface
      config/         # Security and data initialization
      common/exception/ # Global exception handling
    resources/
      application.properties
  test/
    java/com/trekha/trekha/
```

## Getting Started

### Prerequisites

- Java 21
- Maven
- MySQL

### Setup

1. **Clone the repository**
2. **Configure the database**  
   Update `src/main/resources/application.properties` with your MySQL credentials if needed.
3. **Build the project**

   ```sh
   ./mvnw clean install
   ```

4. **Run the application**

   ```sh
   ./mvnw spring-boot:run
   ```

5. **Access API documentation**  
   Visit [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## API Endpoints

- `POST /api/v1/auth/register/passenger/email`  
  Register a passenger using email.
- `POST /api/v1/auth/register/passenger/mobile`  
  Register a passenger using mobile number.

## Technologies Used

- Spring Boot
- Spring Data JPA
- Spring Security
- Lombok
- MySQL
- Swagger/OpenAPI

## Development

- Use Lombok annotations for boilerplate code reduction (getters, setters, etc.).
- Exception handling is centralized in [`GlobalExceptionHandler`](src/main/java/com/app/trekha/common/exception/GlobalExceptionHandler.java).
- Roles are initialized at startup by [`DataInitializer`](src/main/java/com/app/trekha/config/DataInitializer.java).

## License

This project is for commercial purpose
