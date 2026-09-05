# Payment Gateway Quarkus statistics E2E

POST {{base_url}}/api/auth/login
Content-Type: application/json

{"email":"{{stats_email}}","password":"E2E-password-123"}
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
jsonpath "$.data.totalBalance" == 1000000

POST {{base_url}}/api/merchants
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"name":"{{stats_merchant}}","userId":{{user_id}}}
HTTP 201
[Captures]
merchant_id: jsonpath "$.data.id"
api_key: jsonpath "$.data.apiKey"
[Asserts]
jsonpath "$.data.id" exists

POST {{base_url}}/api/topups
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{card_number}}","topupAmount":200000,"topupMethod":"BANK_TRANSFER","idempotencyKey":"stats-topup-{{user_id}}"}
HTTP 201
[Asserts]
jsonpath "$.status" == "success"

POST {{base_url}}/api/transactions
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"apiKey":"{{api_key}}","cardNumber":"{{card_number}}","amount":50000,"paymentMethod":"BANK_TRANSFER","merchantId":{{merchant_id}},"idempotencyKey":"stats-txn-{{user_id}}"}
HTTP 201
[Asserts]
jsonpath "$.status" == "success"

POST {{base_url}}/api/withdraws
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{card_number}}","withdrawAmount":60000,"idempotencyKey":"stats-withdraw-{{user_id}}"}
HTTP 201
[Asserts]
jsonpath "$.status" == "success"

# Every stats endpoint below is a real route declared in gateway resources.
GET {{base_url}}/api/saldos/stats/balance/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/saldos/stats/balance/yearly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/saldos/stats/total/monthly?year=2026&month=1
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/cards/stats/balance/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/cards/stats/balance/monthly/by-card?year=2026&cardNumber={{card_number}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/cards/stats/topup/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/cards/stats/transaction/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/cards/stats/withdraw/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/cards/stats/transfer/monthly/sender?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/cards/dashboard
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/topups/stats/amount/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/topups/stats/method/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/topups/stats/status/monthly/success?year=2026&month=1
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/transactions/stats/amount/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/transactions/stats/method/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/transactions/stats/status/monthly/success?year=2026&month=1
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/transfers/stats/amount/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/transfers/stats/status/monthly/success?year=2026&month=1
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/withdraws/stats/amount/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/withdraws/stats/status/monthly/success?year=2026&month=1
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/merchants/stats/amount/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/merchants/stats/method/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/merchants/stats/total-amount/monthly?year=2026
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"
