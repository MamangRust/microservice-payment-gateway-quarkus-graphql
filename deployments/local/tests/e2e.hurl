# Payment Gateway Quarkus REST E2E
# The runner creates the user and grants ROLE_ADMIN in the local test database.

GET {{base_url}}/q/health
HTTP 200
[Asserts]
jsonpath "$.status" == "UP"

GET {{base_url}}/q/health/live
HTTP 200
[Asserts]
jsonpath "$.status" == "UP"

GET {{base_url}}/q/health/ready
HTTP 200
[Asserts]
jsonpath "$.status" == "UP"

POST {{base_url}}/api/auth/login
Content-Type: application/json

{"email":"{{e2e_email}}","password":"E2E-password-123"}
HTTP 200
[Captures]
access_token: jsonpath "$.data.accessToken"
refresh_token: jsonpath "$.data.refreshToken"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.accessToken" exists
jsonpath "$.data.refreshToken" exists

GET {{base_url}}/api/auth/me?userId={{user_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.id" == {{user_id}}
jsonpath "$.data.email" == "{{e2e_email}}"

POST {{base_url}}/api/cards
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"userId":{{user_id}},"cardType":"VISA","expireDate":"2035-12-31","cvv":"123","cardProvider":"BCA"}
HTTP 201
[Captures]
card_id: jsonpath "$.data.id"
card_number: jsonpath "$.data.cardNumber"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.id" exists
jsonpath "$.data.cardNumber" exists

GET {{base_url}}/api/cards/{{card_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{card_id}}
jsonpath "$.data.cardNumber" == "{{card_number}}"

GET {{base_url}}/api/cards/by-number/{{card_number}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.cardNumber" == "{{card_number}}"

POST {{base_url}}/api/saldos
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{card_number}}","totalBalance":1000000}
HTTP 201
[Captures]
saldo_id: jsonpath "$.data.saldoId"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.saldoId" exists

PUT {{base_url}}/api/saldos/{{saldo_id}}
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{card_number}}","totalBalance":1000000}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.totalBalance" == 1000000

GET {{base_url}}/api/saldos/{{saldo_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.saldoId" == {{saldo_id}}

POST {{base_url}}/api/merchants
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"name":"{{e2e_merchant}}","userId":{{user_id}}}
HTTP 201
[Captures]
merchant_id: jsonpath "$.data.id"
api_key: jsonpath "$.data.apiKey"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.id" exists
jsonpath "$.data.apiKey" exists

GET {{base_url}}/api/merchants/{{merchant_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{merchant_id}}
jsonpath "$.data.apiKey" == "{{api_key}}"

POST {{base_url}}/api/merchant-documents
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"merchantId":{{merchant_id}},"documentType":"BUSINESS_LICENSE","documentUrl":"https://example.test/{{user_id}}.pdf"}
HTTP 201
[Captures]
document_id: jsonpath "$.data.documentId"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.documentId" exists

GET {{base_url}}/api/merchant-documents/{{document_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.documentId" == {{document_id}}

POST {{base_url}}/api/topups
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{card_number}}","topupAmount":200000,"topupMethod":"BANK_TRANSFER","idempotencyKey":"e2e-topup-{{user_id}}"}
HTTP 201
[Captures]
topup_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.id" exists

GET {{base_url}}/api/topups/{{topup_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{topup_id}}

POST {{base_url}}/api/transactions
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"apiKey":"{{api_key}}","cardNumber":"{{card_number}}","amount":50000,"paymentMethod":"BANK_TRANSFER","merchantId":{{merchant_id}},"idempotencyKey":"e2e-txn-{{user_id}}"}
HTTP 201
[Captures]
transaction_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.id" exists

GET {{base_url}}/api/transactions/{{transaction_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{transaction_id}}

POST {{base_url}}/api/withdraws
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{card_number}}","withdrawAmount":60000,"idempotencyKey":"e2e-withdraw-{{user_id}}"}
HTTP 201
[Captures]
withdraw_id: jsonpath "$.data.withdrawId"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.withdrawId" exists

GET {{base_url}}/api/withdraws/{{withdraw_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.withdrawId" == {{withdraw_id}}

# Second card creates real transfer prerequisites.
POST {{base_url}}/api/cards
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"userId":{{user_id}},"cardType":"MASTERCARD","expireDate":"2035-12-31","cvv":"456","cardProvider":"BCA"}
HTTP 201
[Captures]
receiver_card_number: jsonpath "$.data.cardNumber"
[Asserts]
jsonpath "$.data.cardNumber" exists

POST {{base_url}}/api/saldos
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{receiver_card_number}}","totalBalance":100000}
HTTP 201
[Captures]
receiver_saldo_id: jsonpath "$.data.saldoId"
[Asserts]
jsonpath "$.status" == "success"

PUT {{base_url}}/api/saldos/{{receiver_saldo_id}}
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{receiver_card_number}}","totalBalance":100000}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

POST {{base_url}}/api/transfers
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"transferFrom":"{{card_number}}","transferTo":"{{receiver_card_number}}","transferAmount":50000,"idempotencyKey":"e2e-transfer-{{user_id}}"}
HTTP 201
[Captures]
transfer_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.id" exists

GET {{base_url}}/api/transfers/{{transfer_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{transfer_id}}

POST {{base_url}}/api/auth/refresh
Content-Type: application/json

{"refreshToken":"{{refresh_token}}"}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.accessToken" exists
