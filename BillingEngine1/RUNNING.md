# Billing Engine — Running Guide

## Prerequisites

| Tool  | Version |
|-------|---------|
| Java  | 17+     |
| Maven | 3.9+    |

cmd:: Step 1 – confirm Java works
java -version

:: Step 2 – confirm Maven works  
mvn -version

:: Step 3 – go into the project folder BillingEngine1

## Start the service

mvn spring-boot:run
```
Service starts on **http://localhost:8080**

---

## Swagger UI

Open in your browser once the service is running:

```
http://localhost:8080/swagger-ui.html
```

Raw OpenAPI JSON:

```
http://localhost:8080/api-docs

---

## Canned data

| Policy   | Holder        | Plan                        | Billing    | Annual premium |
|----------|---------------|-----------------------------|------------|----------------|
| POL-1001 | Alice Johnson | Life Assurance – Gold       | QUARTERLY  | £1,280.00      |
| POL-1002 | Bob Williams  | Home & Contents – Silver    | MONTHLY    | £1,746.00      |
| POL-1003 | Carol Smith   | Critical Illness – Platinum | BIANNUAL   | £1,780.00      |

**Simulation rule for payment outcomes:**

| paymentMethodToken | Result  |
|--------------------|---------|
| Ends in `fail`     | FAILED  |
| Anything else      | SUCCESS |

---

## Endpoints

### 1. Retrieve premium schedule

```
GET /api/v1/billing/policies/{policyId}/premium-schedule
```

```bash
# Quarterly policy – 4 instalments
curl -s http://localhost:8080/api/v1/billing/policies/POL-1001/premium-schedule | jq .

# Monthly policy – 12 instalments
curl -s http://localhost:8080/api/v1/billing/policies/POL-1002/premium-schedule | jq .

# Unknown policy – 404
curl -s http://localhost:8080/api/v1/billing/policies/POL-UNKNOWN/premium-schedule | jq .
```

**200 response:**
```json
{
  "policyId": "POL-1001",
  "planType": "Life Assurance – Gold",
  "holderName": "Alice Johnson",
  "annualPremium": 1280.00,
  "billingFrequency": "QUARTERLY",
  "schedule": [
    { "installmentNumber": 1, "dueDate": "2025-01-01", "amount": 320.00, "status": "PAID" },
    { "installmentNumber": 2, "dueDate": "2025-04-01", "amount": 320.00, "status": "PAID" },
    { "installmentNumber": 3, "dueDate": "2025-07-01", "amount": 320.00, "status": "OVERDUE" },
    { "installmentNumber": 4, "dueDate": "2025-10-01", "amount": 320.00, "status": "PENDING" }
  ]
}
```

---

### 2. Record a payment attempt

```
POST /api/v1/billing/payments/attempts
Content-Type: application/json
```

```bash
# Successful payment
curl -s -X POST http://localhost:8080/api/v1/billing/payments/attempts \
  -H "Content-Type: application/json" \
  -d '{"policyId":"POL-1001","amount":320.00,"paymentMethodToken":"tok_visa_4242"}' | jq .

# Failed payment (token ends in 'fail')
curl -s -X POST http://localhost:8080/api/v1/billing/payments/attempts \
  -H "Content-Type: application/json" \
  -d '{"policyId":"POL-1002","amount":145.50,"paymentMethodToken":"tok_card_fail"}' | jq .

# Validation error
curl -s -X POST http://localhost:8080/api/v1/billing/payments/attempts \
  -H "Content-Type: application/json" \
  -d '{"policyId":"","amount":0,"paymentMethodToken":"tok"}' | jq .
```

**201 SUCCESS response:**
```json
{
  "attemptId": "ATT-A1B2C3D4",
  "policyId": "POL-1001",
  "amount": 320.00,
  "status": "SUCCESS",
  "gatewayReference": "GW-9F8E7D6C",
  "failureReason": null,
  "attemptedAt": "2025-08-10T14:22:01.123Z",
  "attemptNumber": 1,
  "nextRetryScheduledAt": null
}
```

**201 FAILED response:**
```json
{
  "attemptId": "ATT-E5F6G7H8",
  "policyId": "POL-1002",
  "amount": 145.50,
  "status": "FAILED",
  "gatewayReference": null,
  "failureReason": "Insufficient funds – simulated failure",
  "attemptedAt": "2025-08-10T14:22:05.456Z",
  "attemptNumber": 1,
  "nextRetryScheduledAt": "2025-08-11T14:22:05.456Z"
}
```

---

### 3. List delinquent policies

```
GET /api/v1/billing/policies/delinquent
```

```bash
curl -s http://localhost:8080/api/v1/billing/policies/delinquent | jq .
```

**200 response:**
```json
{
  "totalCount": 3,
  "policies": [
    {
      "policyId": "POL-1001",
      "holderId": "HLD-501",
      "holderName": "Alice Johnson",
      "delinquencyStatus": "WARNING",
      "daysPastDue": 45,
      "outstandingBalance": 320.00,
      "failedAttempts": 2,
      "lastAttemptDate": "2025-08-07",
      "nextRetryDate": "2025-08-14"
    }
  ]
}
```

---

### 4. Trigger a retry

```
POST /api/v1/billing/payments/retry/{policyId}
```

```bash
# Queue a retry for POL-1002 (run after a failed attempt)
curl -s -X POST http://localhost:8080/api/v1/billing/payments/retry/POL-1002 | jq .

# Unknown policy – 404
curl -s -X POST http://localhost:8080/api/v1/billing/payments/retry/POL-9999 | jq .
```

**202 response:**
```json
{
  "policyId": "POL-1002",
  "retryJobId": "JOB-I9J0K1L2",
  "scheduledAt": "2025-08-11T14:25:00.000Z",
  "attemptNumber": 2,
  "message": "Retry job queued – attempt #2 in 1 day(s)"
}
```

**Retry back-off ladder:**

| Attempt transition | Delay   |
|--------------------|---------|
| 0 → 1              | +1 day  |
| 1 → 2              | +1 day  |
| 2 → 3              | +3 days |
| 3 → 4              | +7 days |
| > 3                | 422 – escalate to collections |

---

## Run tests

```bash
./mvnw test
```

Expected: **17 tests, 0 failures.**

---

## Project layout

```
billing-engine/
├── pom.xml
├── RUNNING.md
├── requests.http
└── src/
    ├── main/
    │   ├── java/com/insurance/billing/
    │   │   ├── BillingEngineApplication.java
    │   │   ├── config/
    │   │   │   └── OpenApiConfig.java            ← Swagger / OpenAPI setup
    │   │   ├── controller/
    │   │   │   └── BillingEngineController.java  ← 4 REST endpoints
    │   │   ├── service/
    │   │   │   └── BillingEngineService.java     ← business logic + retry ladder
    │   │   ├── data/
    │   │   │   └── CannedDataStore.java          ← in-memory fixture data
    │   │   ├── model/
    │   │   │   ├── domain/                       ← enums + records
    │   │   │   └── dto/request/ + response/      ← API contracts
    │   │   └── exception/
    │   │       └── GlobalExceptionHandler.java   ← RFC 7807 ProblemDetail
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/insurance/billing/controller/
            └── BillingEngineControllerTest.java  ← 17 JUnit 5 / MockMvc tests
```
