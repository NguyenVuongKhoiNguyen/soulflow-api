The backend uses Java 17 and Spring Boot 3.3.5. 
Area	Technology	Purpose
Language	Java 17	Backend application code
Framework	Spring Boot 3.3.5	Application setup, configuration, and dependency injection
REST API	Spring MVC	Controllers, HTTP endpoints, request/response handling
Security	Spring Security	Authentication and role-based authorization
Access tokens	JJWT 0.11.5	Generate and validate JWTs
Password security	BCrypt	Hash and verify passwords
Google login	Google API Client / OAuth libraries	Verify Google authentication
Database	Microsoft SQL Server 2022	Store accounts, products, orders, and other records
Database access	Spring Data JPA + Hibernate	Map Java entities to tables and implement repositories
Direct SQL	Spring JDBC / JdbcTemplate	Execute SQL directly, including webhook receipt handling
Database driver	Microsoft SQL Server JDBC	Connect Java to SQL Server
Migrations	Flyway	Apply versioned database schema changes
Caching	Spring Cache + Spring Data Redis	Cache application data
Token storage	Redis	Store refresh tokens for rotation and revocation
Object storage	MinIO, SDK 9.0.2	Store product images and other files
Mapping	MapStruct 1.5.5.Final	Convert between requests, entities, and responses
Boilerplate reduction	Lombok	Generate getters, setters, constructors, and related methods
Validation	Jakarta Bean Validation	Validate incoming request fields
JSON	Jackson	Convert Java objects to/from JSON, including date/time values
Real-time updates	Spring WebSocket + STOMP	Send notifications to connected clients
Email	Spring Mail	Send email through SMTP
AI	Spring AI 1.0.0 + Ollama	Integrate locally hosted AI models
Payment notifications	SePay webhook	Receive bank-transfer notifications and update payment status
Payment QR images	VietQR	Build QR image URLs containing payment information
Build system	Maven	Manage dependencies and package the application
Deployment	Docker + Docker Compose	Build and run the API and supporting services
Development utilities	Spring Boot DevTools	Development-time restart support
Testing dependencies	Spring Boot Starter Test	Provides testing tools such as JUnit and Mockito


Your application follows a layered architecture:
Controller → Service → Repository → Database, with DTOs and MapStruct mappers handling data conversion.
SePay and VietQR are external integrations; SQL Server, Redis, MinIO, and Ollama run as supporting services in your Docker Compose setup