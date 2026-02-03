# OpenAPI Generator - Quick Start

## 🚀 Quick Commands

### Generate DTOs
```bash
cd reservation
mvn clean generate-sources
```

### Build Project (Docker - Recommended)
```bash
docker compose build backend
docker compose up -d
```

### View Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### Get OpenAPI JSON
```
http://localhost:8080/v3/api-docs
```

## 📁 Important Files

| File | Purpose |
|------|---------|
| `reservation/src/main/resources/openapi.yml` | **Source of truth** - API specification |
| `reservation/pom.xml` | OpenAPI Generator Maven plugin configuration |
| `reservation/src/main/java/com/example/reservation/config/OpenApiConfig.java` | Loads openapi.yml for Swagger UI |
| `target/generated-sources/openapi/...` | Generated DTOs (not committed to git) |

## 🔧 Common Tasks

### Adding a New Endpoint

1. **Edit openapi.yml:**
```yaml
paths:
  /api/properties/{id}/archive:
    post:
      tags:
        - Properties
      summary: Archive a property
      operationId: archiveProperty
      parameters:
        - $ref: '#/components/parameters/PropertyId'
      responses:
        '200':
          description: Property archived
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PropertyResponse'
```

2. **Regenerate DTOs:**
```bash
mvn clean generate-sources
```

3. **Implement controller:**
```java
@PostMapping("/{id}/archive")
public PropertyResponse archiveProperty(@PathVariable UUID id) {
    return PropertyResponse.from(propertyService.archive(id));
}
```

### Adding a New DTO

1. **Define schema in openapi.yml:**
```yaml
components:
  schemas:
    PropertyArchiveRequest:
      type: object
      required:
        - reason
      properties:
        reason:
          type: string
          maxLength: 255
          description: Reason for archiving
```

2. **Reference in endpoint:**
```yaml
requestBody:
  required: true
  content:
    application/json:
      schema:
        $ref: '#/components/schemas/PropertyArchiveRequest'
```

3. **Regenerate:**
```bash
mvn clean generate-sources
```

### Adding Validation

In openapi.yml:
```yaml
properties:
  email:
    type: string
    format: email
    maxLength: 255
  age:
    type: integer
    minimum: 18
    maximum: 120
  price:
    type: number
    format: decimal
    minimum: 0.01
```

Generates:
```java
@Email
@Size(max = 255)
private String email;

@Min(18)
@Max(120)
private Integer age;

@DecimalMin("0.01")
private BigDecimal price;
```

## 🐛 Troubleshooting

### DTOs Not Generated

**Check:**
```bash
mvn clean generate-sources
ls target/generated-sources/openapi/src/main/java/com/example/reservation/dto/generated/
```

**If empty:**
- Check `openapi.yml` syntax: https://editor.swagger.io/
- Check Maven output for errors

### IDE Doesn't Recognize Generated Classes

**IntelliJ:**
1. Right-click `target/generated-sources/openapi/src/main/java`
2. Mark Directory as → Generated Sources Root
3. Rebuild project

**VS Code:**
1. Reload window
2. Clean workspace
3. Rebuild

### Compilation Fails (Java 21 Required)

**Use Docker:**
```bash
docker compose build backend
```

**Or install Java 21:**
```bash
# macOS
brew install openjdk@21

# Ubuntu
sudo apt install openjdk-21-jdk
```

### Swagger UI Shows Old Spec

**Clear cache:**
```bash
mvn clean
docker compose down
docker compose up --build
```

## 📊 Pagination

All list endpoints support:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `unpaged` | boolean | false | Return all results (ignores pagination) |
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 20 | Page size (max: 100) |
| `sort` | string | createdAt | Sort field and direction |

**Examples:**
```bash
# Default pagination (page 0, size 20)
GET /api/properties

# Custom page size
GET /api/properties?page=2&size=50

# Sort descending
GET /api/properties?sort=createdAt,desc

# All results (no pagination)
GET /api/properties?unpaged=true

# Combine parameters (unpaged takes precedence)
GET /api/properties?unpaged=true&page=5  # Returns all results, ignores page
```

## 🔐 Authentication

**Get JWT token:**
```bash
curl -X POST 'http://localhost:8081/realms/reservation/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=reservation-test' \
  -d 'username=testuser' \
  -d 'password=testuser'
```

**Extract token:**
```bash
export TOKEN=$(curl -X POST 'http://localhost:8081/realms/reservation/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=reservation-test&username=testuser&password=testuser' \
  -s | jq -r .access_token)
```

**Use token:**
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/reservations/mine
```

**In Swagger UI:**
1. Click "Authorize" button
2. Paste token (without "Bearer " prefix)
3. Click "Authorize"

## 📝 API Testing Examples

### Create Property
```bash
curl -X POST http://localhost:8080/api/properties \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Appartement Paris 8ème",
    "description": "Bel appartement avec vue",
    "city": "Paris",
    "pricePerNight": 150.00
  }'
```

### List Properties
```bash
# Public endpoint (no auth)
curl http://localhost:8080/api/properties

# With pagination
curl "http://localhost:8080/api/properties?page=0&size=10"

# Unpaged
curl "http://localhost:8080/api/properties?unpaged=true"

# Filter by city
curl "http://localhost:8080/api/properties?city=Paris"
```

### Create Reservation
```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": "550e8400-e29b-41d4-a716-446655440000",
    "startDate": "2024-06-15",
    "endDate": "2024-06-20"
  }'
```

### My Reservations
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/reservations/mine
```

## 🎨 Generated DTO Structure

### Request DTOs
```java
// PropertyCreateRequest
{
  "title": "string",          // @NotNull @Size(max=120)
  "description": "string",    // @NotNull @Size(max=2000)
  "city": "string",           // @NotNull @Size(max=120)
  "pricePerNight": 150.00     // @NotNull @DecimalMin("0.01")
}
```

### Response DTOs
```java
// PropertyResponse
{
  "id": "uuid",
  "ownerSub": "string",
  "title": "string",
  "description": "string",
  "city": "string",
  "pricePerNight": 150.00,
  "status": "ACTIVE",         // Enum
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

### Paginated Response
```java
// PageResponsePropertyListResponse
{
  "content": [...],           // Array of PropertyListResponse
  "page": 0,                  // Current page (0-indexed)
  "size": 20,                 // Page size
  "totalElements": 100,       // Total items
  "totalPages": 5,            // Total pages
  "first": true,              // Is first page?
  "last": false               // Is last page?
}
```

### Error Response
```java
// ProblemDetail (RFC 7807)
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "title: must not be blank",
  "instance": "/api/properties"
}
```

## 📚 Documentation Files

| File | Purpose |
|------|---------|
| `OPENAPI_QUICK_START.md` | This file - Quick reference |
| `OPENAPI_GENERATOR.md` | Comprehensive guide (300+ lines) |
| `OPENAPI_IMPLEMENTATION_SUMMARY.md` | Implementation details |
| `CLAUDE.md` | Project context |

## 🔗 Useful Links

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **Health Check:** http://localhost:8080/actuator/health
- **OpenAPI Editor:** https://editor.swagger.io/
- **OpenAPI Spec:** https://swagger.io/specification/
- **Generator Docs:** https://openapi-generator.tech/

## 💡 Tips

1. **Always regenerate DTOs after editing openapi.yml:**
   ```bash
   mvn clean generate-sources
   ```

2. **Use Swagger Editor to validate openapi.yml:**
   - Copy content from `openapi.yml`
   - Paste into https://editor.swagger.io/
   - Check for errors

3. **Keep DTOs separate from entities:**
   - DTOs: API layer (`dto.generated` package)
   - Entities: Domain layer (`domain` package)
   - Use mappers to convert between them

4. **Don't modify generated files:**
   - They're regenerated on each build
   - Modify `openapi.yml` instead

5. **Use $ref for reusability:**
   ```yaml
   # Define once
   components:
     parameters:
       PropertyId:
         name: id
         in: path
         required: true
         schema:
           type: string
           format: uuid

   # Reference everywhere
   paths:
     /api/properties/{id}:
       get:
         parameters:
           - $ref: '#/components/parameters/PropertyId'
   ```

## 🎯 Workflow

```
┌─────────────────┐
│  Edit           │
│  openapi.yml    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  mvn generate-  │
│  sources        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  DTOs Generated │
│  in target/...  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Implement      │
│  Controller     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Test via       │
│  Swagger UI     │
└─────────────────┘
```

## ✅ Checklist for New Endpoints

- [ ] Define path in `openapi.yml`
- [ ] Define request/response schemas
- [ ] Add validation rules
- [ ] Document parameters
- [ ] Define error responses
- [ ] Run `mvn clean generate-sources`
- [ ] Verify DTOs generated
- [ ] Implement controller method
- [ ] Test via Swagger UI
- [ ] Test authentication (if required)
- [ ] Test validation (with invalid data)

---

**Need more details?** See `OPENAPI_GENERATOR.md` for comprehensive documentation.
