# Quarkus financial risk-path smoke flow.
# The current gateway has no REST fraud-scoring or /api/v1 credit-account route;
# this suite verifies the real card, merchant, transaction and dashboard paths.

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
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/auth/me?userId={{user_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{user_id}}

POST {{base_url}}/api/cards
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"userId":{{user_id}},"cardType":"VISA","expireDate":"2035-12-31","cvv":"123","cardProvider":"BCA"}
HTTP 201
[Captures]
card_number: jsonpath "$.data.cardNumber"
[Asserts]
jsonpath "$.data.cardNumber" exists

POST {{base_url}}/api/saldos
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{card_number}}","totalBalance":1000000}
HTTP 201
[Captures]
saldo_id: jsonpath "$.data.saldoId"
[Asserts]
jsonpath "$.status" == "success"

PUT {{base_url}}/api/saldos/{{saldo_id}}
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{card_number}}","totalBalance":1000000}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

POST {{base_url}}/api/merchants
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"name":"Fraud smoke {{user_id}}","userId":{{user_id}}}
HTTP 201
[Captures]
merchant_id: jsonpath "$.data.id"
api_key: jsonpath "$.data.apiKey"
[Asserts]
jsonpath "$.data.id" exists

POST {{base_url}}/api/transactions
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"apiKey":"{{api_key}}","cardNumber":"{{card_number}}","amount":100000,"paymentMethod":"BANK_TRANSFER","merchantId":{{merchant_id}},"idempotencyKey":"fraud-smoke-{{user_id}}"}
HTTP 201
[Captures]
transaction_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/transactions/{{transaction_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{transaction_id}}

GET {{base_url}}/api/cards/dashboard/{{card_number}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"
