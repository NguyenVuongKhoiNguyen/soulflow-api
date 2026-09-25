# Data flow when update

Frontend → Controller → Service → Mapper → Repository → Database → Response
Suppose the frontend wants to update an existing order.
1. The frontend sends the order ID and editable fields
   The ID identifies the existing row. Other fields describe the requested changes, such as the delivery address or order items.
2. Spring converts the JSON into an OrderRequest
   The controller receives it through @RequestBody. If @Valid is present, Spring validates the request before executing the controller method.
3. The controller calls the service
   It passes the DTO to orderService.save(request). The service’s @Transactional starts or joins a transaction covering the database work.
4. The service calls the mapper
   Your service calls orderMapper.toEntity(request).
   MapStruct creates an Order entity and automatically copies the mapped fields. Fields marked ignore = true—such as createdDate, account, total, and payment—are skipped at this stage.
5. The mapper’s @AfterMapping method runs
   This is the part you remembered. Because the request contains an ID, your mapper follows its update branch:
   - Loads the existing order from the database.
   - Throws an error if it doesn’t exist.
   - Copies preserved fields from the old order, including its creation date, account, expiration information, and deleted flag.
   - Recalculates the total from the mapped order details.
   - Synchronizes the payment with the updated order.
   Your current order mapper also performs inventory-related work.
   These fields are not automatically immutable. ignore = true only prevents automatic copying. Your @AfterMapping logic decides which values to preserve, initialize, or recalculate.
6. The service calls orderRepository.save(order)
   Because this entity represents an existing row, Spring Data JPA normally uses EntityManager.merge().
   Hibernate copies its state into a managed entity. The object returned by save() is the managed result, which is why your service uses the returned saved entity.
7. Hibernate flushes changes to the database
   Hibernate generates the necessary SQL updates. Related records may also change according to your relationship mappings and cascade settings.
   SQL execution can happen before commit, but the changes become permanent only when the transaction commits. A failure that triggers rollback undoes the transaction’s database changes.
8. The saved entity is mapped into an OrderResponse
   The service calls orderMapper.toResponse(saved). This produces the fields the frontend should receive, rather than exposing the JPA entity directly.
   Response-side @AfterMapping methods can also add calculated fields, such as the payment reference or QR URL.
9. Caching and the HTTP response are handled
   Your service’s cache annotations update or invalidate relevant cached data. After successful service completion, the controller returns the response DTO, and Spring serializes it to JSON.