# Quarkus payment lifecycle smoke flow.
# The current gateway exposes card/topup/transaction resources; it does not expose
# the former /api/v1/cards credit-account RPCs.

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
card_id: jsonpath "$.data.id"
card_number: jsonpath "$.data.cardNumber"
[Asserts]
jsonpath "$.status" == "success"

POST {{base_url}}/api/saldos
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{card_number}}","totalBalance":500000}
HTTP 201
[Captures]
saldo_id: jsonpath "$.data.saldoId"
[Asserts]
jsonpath "$.status" == "success"

PUT {{base_url}}/api/saldos/{{saldo_id}}
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{card_number}}","totalBalance":500000}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.totalBalance" == 500000

POST {{base_url}}/api/topups
Authorization: Bearer {{access_token}}
Content-Type: application/json

{"cardNumber":"{{card_number}}","topupAmount":100000,"topupMethod":"BANK_TRANSFER","idempotencyKey":"clc-topup-{{user_id}}"}
HTTP 201
[Captures]
topup_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/topups/{{topup_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.id" == {{topup_id}}

GET {{base_url}}/api/cards/{{card_id}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.data.cardNumber" == "{{card_number}}"

GET {{base_url}}/api/cards/stats/topup/monthly/by-card?year=2035&cardNumber={{card_number}}
Authorization: Bearer {{access_token}}
HTTP 200
[Asserts]
jsonpath "$.status" == "success"
