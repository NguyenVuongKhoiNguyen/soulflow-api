# Payment data flow

1. Customer places an order
   The frontend sends the order to POST /checkout/orders, including the products, quantities, and E_BANKING payment method.
   CheckoutController passes the request to OrderService. The mapper builds the order and its details, calculates the total, and creates the payment record.
   SQL Server stores:
   - Order status: PENDING
   - Payment method: E_BANKING
   - Payment paid: false
2. Backend generates the QR URL
   After saving, the order has an ID—for example, 123.
   Your payment reference becomes DH123. QrService builds a VietQR image URL containing the receiving bank account, amount, reference, and account name.
   The backend returns the order ID, total, payment reference, and QR URL to the frontend.
   Generating the QR does not create a transaction or confirm payment. It provides the bank-transfer details.
3. Customer scans and pays
   The frontend displays the QR image. The customer scans it using their banking app and confirms the transfer.
   The money goes directly to your configured bank account. The transfer description contains DH123, allowing your backend to identify the order.
4. SePay sends the transaction to your backend
   SePay sends POST /webhook/sepay with transaction information, including the transaction ID, receiving account, transfer amount, and transfer description.
   NonUserController checks the webhook API key through SepayWebhookService, then passes the transaction for processing.
5. Backend validates and matches the payment
   Your service checks that:
   - The receiving bank account matches your configuration.
   - The transaction is incoming.
   - The SePay transaction has not already been processed.
   - The reference identifies an existing order.
   - The order is pending, unpaid, available, and unexpired.
   - Its payment method is E_BANKING.
   - The transferred amount exactly matches the order total.
   The webhook payload is stored in sepay_webhook_receipts. Failed matching or business checks are recorded with an outcome such as AMOUNT_MISMATCH; they do not mark the order paid.
6. Backend saves the successful payment
   Within one database transaction, it updates:
   - Payment: paid = true, received amount, and payment date.
   - Order: status becomes PAID.
   - Webhook receipt: marked processed with outcome PAID.
   After the transaction commits, it clears relevant order caches and publishes an order update through WebSocket.
7. Frontend displays success
   Your payment page checks GET /checkout/orders/{id} every 5 seconds while payment is pending.
   This endpoint reads the database directly. When it returns paid: true, the frontend stops polling and displays payment success.
The database payment state is the source of truth. Also, the webhook’s { "success": true } means the notification was handled—it does not necessarily mean an order was marked paid.