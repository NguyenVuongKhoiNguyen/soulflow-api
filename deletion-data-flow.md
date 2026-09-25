
# Data flow when deleting

Frontend → Controller → Service → Repository → Database

For example, deleting an order:
1. The frontend sends a DELETE request
   It sends the order ID in the URL, such as /user/order/5. A request body usually isn’t needed.
2. The controller receives the ID
   @PathVariable extracts 5. The controller calls something like orderService.softDeleteById(5).
   Spring Security checks route access before the controller executes. Ownership checks, when required, belong in the service.
3. The service handles the operation
   It applies relevant business rules and calls the repository inside a transaction.
   Your existing order deletion service directly calls orderRepo.softDelete(orderId). It doesn’t load and map the order first.
4. The repository updates the deleted flag
   Your repository uses a modifying query equivalent to:
   UPDATE orders
   SET del_if = 1
   WHERE id = 5;
   This is an UPDATE, even though the HTTP request uses the DELETE method.
5. The transaction commits
   The row remains, including its relationships and historical information. Normal listing queries must exclude deleted rows to hide it.
6. The service invalidates affected caches
   Your order deletion service clears the relevant order-detail, order-page, and product-page cache entries so subsequent requests can load updated data.
7. The controller returns a response
   There’s usually no response DTO. An explicitly configured 204 No Content is common; a void method alone doesn’t automatically guarantee that status.