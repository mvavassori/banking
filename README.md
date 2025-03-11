# Banking API - Spring Boot Backend

This project is a robust and secure RESTful API for a banking application, built using Spring Boot. It demonstrates a solid understanding of backend development principles, Java best practices, and modern Spring features.

## Project Overview

This API provides core banking functionalities, including:

- **User Management:** User registration, authentication (with JWT), and profile management. Administrators can manage user accounts.
- **Account Management:** Users can create and manage multiple bank accounts (e.g., checking, savings) with different currencies.
- **Transaction Handling:** The API supports deposits, withdrawals, and transfers between accounts. It provides transaction history retrieval.
- **Security:** Robust security is implemented using Spring Security, JWT (JSON Web Tokens) for authentication, and BCrypt for password hashing. Role-Based Access Control (RBAC) is used to restrict access to certain endpoints (e.g., admin-only operations).
- **Error Handling:** A global exception handling mechanism provides consistent and informative error responses to the client.
- **Data Persistence:** Spring Data JPA is used for interacting with a MySQL database. The application uses Hibernate as the JPA provider.
- **Asynchronous Operations:** Refresh token are implemented.

## Technologies Used

- **Java 21:** The core programming language.
- **Spring Boot 3.4.2:** Provides the foundation for the application, simplifying configuration and dependency management.
- **Spring Security:** Handles authentication, authorization, and overall API security.
- **Spring Data JPA:** Simplifies database interactions.
- **Hibernate:** The JPA implementation used for object-relational mapping.
- **MySQL:** The relational database used to store application data.
- **JWT (JSON Web Tokens):** Used for secure user authentication and authorization.
- **Maven:** Dependency management and build tool.\_

## Getting Started

### Prerequisites

- Java 21 JDK installed.
- Maven installed.
- MySQL server installed and running.
- An IDE (IntelliJ IDEA, Eclipse, VS Code with Java extensions) is highly recommended.

### Setup

1.  **Clone the repository:**

    ```bash
    git clone <repository_url>
    cd banking
    ```

2.  **Configure the database:**

    - Create a MySQL database (e.g., `banking`).
    - Update the `application.properties` file (located in `src/main/resources`) with your database credentials:
      - `spring.datasource.url`: Set the JDBC URL, including the database name. The default assumes a database named `banking` on `localhost:3306`. You'll likely need to change the `${DB_HOST}`, `${DB_PORT}`, and `${DB_NAME}` placeholders. For example:
        ```properties
        spring.datasource.url=jdbc:mysql://localhost:3306/banking?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
        ```
      - `spring.datasource.username`: Your MySQL username (replace `${DB_USER}`).
      - `spring.datasource.password`: Your MySQL password (replace `${DB_PASSWORD}`).
    - Set the JWT secret and expiration times:
      - `jwt.secret`: Replace `${JWT_SECRET}` with a strong, randomly generated secret key. This is _crucial_ for security.
      - `jwt.expiration.accessToken`: Set the access token expiration time (in seconds). Replace `${JWT_ACCESS_TOKEN_EXPIRATION}`.
      - `jwt.expiration.refreshToken`: Set the refresh token expiration time (in seconds). Replace `${JWT_REFRESH_TOKEN_EXPIRATION}`.

3.  **Build and run the application:**

    - Using Maven:
      ```bash
      mvn spring-boot:run
      ```
    - Or, run the `BankingApplication` class (located in `src/main/java/com/marcovavassori/banking`) from your IDE.

4.  **Access the API:**

    The API will be accessible at `http://localhost:8080`. You can use tools like Postman or curl to interact with the API endpoints.

## Code Structure and Navigation (For Reviewers)

The codebase is structured following standard Spring Boot conventions, making it easy to navigate and understand. Here's a recommended approach to reviewing the code:

1.  **Start with the Controllers:** The controller classes define the API endpoints and handle incoming requests. These are located in the `src/main/java/com/marcovavassori/banking/controllers` package. Good starting points are:

    - `AuthenticationController.java`: Handles user registration (`/api/auth/signup`) and login (`/api/auth/signin`).
    - `UserController.java`: Provides endpoints for user profile retrieval (`/api/users/profile`) and admin-level user management.
    - `AccountController.java`: Handles account creation (`/api/accounts`) and retrieval.
    - `TransactionController.java`: Manages transactions (deposits, withdrawals, transfers).

2.  **Explore the Services:** The service classes (in `src/main/java/com/marcovavassori/banking/services`) contain the core business logic. They are called by the controllers. Key service classes include:

    - `AuthenticationService.java`: Handles user registration, login, and token generation.
    - `UserService.java`: Manages user-related operations.
    - `AccountService.java`: Handles account-related business logic.
    - `TransactionService.java`: Implements transaction processing.

3.  **Examine the Models (Entities):** The model classes (in `src/main/java/com/marcovavassori/banking/models`) represent the data entities in the application. These are mapped to database tables using JPA annotations. Key model classes are:

    - `User.java`: Represents a user in the system. Implements `UserDetails` for Spring Security integration.
    - `Account.java`: Represents a bank account.
    - `Transaction.java`: Represents a financial transaction.
    - `RefreshToken.java`: Represents a refresh token.

4.  **Review the Repositories:** The repository interfaces (in `src/main/java/com/marcovavassori/banking/repositories`) provide data access methods. They extend `JpaRepository` to inherit common CRUD operations. Key repositories are:

    - `UserRepository.java`: Provides methods for interacting with user data.
    - `AccountRepository.java`: Provides methods for interacting with account data.
    - `TransactionRepository.java`: Provides methods for interacting with transaction data.
    - `RefreshTokenRepository.java`: Provides methods for interacting with refreshtoken data.

5.  **Understand Security Configuration:** The `SecurityConfig` class (in `src/main/java/com/marcovavassori/banking/config`) configures Spring Security. It defines authentication and authorization rules, password encoding, and JWT filter integration.

6.  **Check Exception Handling:** The `GlobalExceptionHandler` class (in `src/main/java/com/marcovavassori/banking/exceptions`) provides centralized exception handling for the API. It maps exceptions to appropriate HTTP status codes and error responses.

7.  **Review DTOs and Mappers:** DTOs (Data Transfer Objects) are in the package `src/main/java/com/marcovavassori/banking/dtos`. Mappers are in the package `src/main/java/com/marcovavassori/banking/mappers`.

## Key Features and Design Choices

- **RESTful API Design:** The API follows REST principles, using standard HTTP methods (GET, POST, PUT, DELETE) and resource-based URLs.
- **JWT Authentication:** Secure authentication is implemented using JWT, providing stateless and scalable user sessions.
- **Role-Based Access Control (RBAC):** Different user roles (ADMIN, USER) have different levels of access to API resources.
- **Password Hashing:** BCrypt is used to securely hash user passwords, protecting against security breaches.
- **Global Exception Handling:** A centralized exception handler ensures consistent error responses and improves maintainability.
- **Data Validation:** Input validation is performed to prevent invalid data from being processed.
- **Spring Data JPA:** Simplifies database interactions and reduces boilerplate code.
- **Clear Separation of Concerns:** The code is organized into distinct layers (controllers, services, models, repositories), promoting modularity and maintainability.
- **Use of DTOs:** Data Transfer Objects (DTOs) are used to decouple the API from the internal data model, improving flexibility and security.
- **Custom exceptions:** Custom exceptions are used to provide meaningful error messages.
- **Unit Tests:** Comprehensive unit tests are provided for the service layer, ensuring the correctness of the business logic. Key tested classes include `UserService`, `AccountService`, and `TransactionService`.
