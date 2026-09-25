
# Data flow when inserting
Frontend → Controller → Service → Mapper → Repository → Database
Imagine creating a category with a name and description.
1. The frontend sends a POST request
   The request body contains JSON with the category’s fields.
2. Spring converts the JSON into a request DTO
   @RequestBody CategoryRequest tells Spring which Java object to create. If the controller uses @Valid, Spring also checks the DTO’s validation rules before running the method.
3. The controller calls the service
   It passes the CategoryRequest to something like categoryService.save(request). The controller handles the HTTP request; the service handles the operation.
4. The service checks business rules
   For example: is this category name already used? Is the current user allowed to create it? For writes, @Transactional typically defines the database transaction.
5. The mapper converts the request into an entity
   CategoryRequest becomes a Category entity. The entity represents a database table row. For a new row, its generated ID should normally be null.
6. The service calls repository.save(entity)
   Spring Data JPA recognizes the entity as new and asks Hibernate to persist it. Hibernate generates the SQL INSERT, using the entity’s table and column mappings.
7. The database inserts the row
   It enforces constraints, such as required fields and unique values, and generates the ID.
   The INSERT may execute during save() or later when Hibernate flushes. The change becomes permanent when the transaction commits. If the transaction rolls back, the row is not retained.
