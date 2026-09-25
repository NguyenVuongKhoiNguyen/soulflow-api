# Building a controlelr

Define the request and response DTOs
- models/requests: fields the client can submit.
- models/responses: fields the client should receive.
- Put input validation on request fields.
- Avoid returning JPA entities directly.

Create or extend the repository
- Place it in models/repositories.
- Extend JpaRepository.
- Add database queries here, rather than in the controller.

Create the mapper
- Place it in models/mappers.
- Use your existing MapStruct conventions to convert entities and DTOs.
- Prefer keeping business decisions in the service.

Define the service interface and implementation
- Interface: models/services.
- Implementation: models/services/impl.
- Handle business rules, ownership checks, transactions, and repository calls.
- Apply caching and cache invalidation where appropriate.

Add the controller endpoint
- Accept and validate the request.
- Obtain the authenticated identity when needed.
- Call the service.
- Return the response DTO with the appropriate HTTP status.
- Keep database access and business logic out of the controller.

