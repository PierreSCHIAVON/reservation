# OpenAPI Generator Integration

## Overview

This project uses **OpenAPI Generator** to auto-generate DTOs from a centralized `openapi.yml` specification file. The OpenAPI specification is the **single source of truth** for the API contract.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         openapi.yml                             │
│                    (Source of Truth)                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ├──────────────────────────────────┐
                              ▼                                  ▼
                    ┌──────────────────┐           ┌──────────────────────┐
                    │  OpenAPI         │           │  springdoc-openapi  │
                    │  Generator       │           │  (Swagger UI)        │
                    │  Maven Plugin    │           │                      │
                    └──────────────────┘           └──────────────────────┘
                              │                                  │
                              ▼                                  ▼
                    ┌──────────────────┐           ┌──────────────────────┐
                    │  Generated DTOs  │           │  /swagger-ui.html    │
                    │  (com.example.   │           │  /v3/api-docs        │
                    │   reservation.   │           │                      │
                    │   dto.generated) │           │                      │
                    └──────────────────┘           └──────────────────────┘
```

## File Structure

```
reservation/
├── src/main/resources/
│   └── openapi.yml                           # Source of truth for API
├── src/main/java/com/example/reservation/
│   ├── config/
│   │   └── OpenApiConfig.java                # Loads openapi.yml for Swagger UI
│   ├── dto/                                  # Existing DTOs (manual)
│   │   ├── PropertyDto.java
│   │   ├── ReservationDto.java
│   │   └── ...
│   └── controller/                           # Controllers (use existing DTOs for now)
│       ├── PropertyController.java
│       └── ...
└── target/generated-sources/openapi/
    └── src/main/java/com/example/reservation/
        └── dto/generated/                    # Generated DTOs (auto-generated)
            ├── PropertyCreateRequest.java
            ├── PropertyResponse.java
            ├── PageResponsePropertyListResponse.java
            └── ...
```

## Key Components

### 1. `openapi.yml`

Location: `reservation/src/main/resources/openapi.yml`

This is the **single source of truth** for the API. It contains:
- API metadata (title, version, description)
- Server configurations
- Security schemes (JWT Bearer Auth)
- All API paths and operations
- Request/Response schemas (DTOs)
- Pagination parameters
- Error responses (ProblemDetail)

### 2. OpenAPI Generator Maven Plugin

Location: `reservation/pom.xml`

Configuration:
```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.12.0</version>
    <configuration>
        <inputSpec>${project.basedir}/src/main/resources/openapi.yml</inputSpec>
        <generatorName>spring</generatorName>
        <modelPackage>com.example.reservation.dto.generated</modelPackage>

        <!-- Generate models (DTOs) only -->
        <generateApis>false</generateApis>
        <generateModels>true</generateModels>
        <generateSupportingFiles>false</generateSupportingFiles>

        <!-- Use Jakarta EE and validation -->
        <configOptions>
            <useJakartaEe>true</useJakartaEe>
            <useBeanValidation>true</useBeanValidation>
            <useSpringBoot3>true</useSpringBoot3>
        </configOptions>
    </configuration>
</plugin>
```

**What it does:**
- Reads `openapi.yml`
- Generates Java classes in `com.example.reservation.dto.generated`
- Adds Jakarta validation annotations (@NotNull, @Size, @Valid, etc.)
- Creates proper Jackson serialization annotations
- Does NOT generate controllers, APIs, or tests

### 3. Generated DTOs

Location: `target/generated-sources/openapi/src/main/java/com/example/reservation/dto/generated/`

Examples:
- `PropertyCreateRequest.java` - Request DTO for creating properties
- `PropertyResponse.java` - Response DTO for property details
- `PropertyListResponse.java` - Response DTO for property lists
- `PageResponsePropertyListResponse.java` - Paginated response wrapper
- `PropertyStatus.java` - Enum for property status
- `ProblemDetail.java` - Error response format

**Features:**
- ✅ Jakarta validation annotations
- ✅ Jackson JSON serialization
- ✅ Proper constructors, getters, setters
- ✅ equals(), hashCode(), toString()
- ✅ Builder pattern support
- ❌ No Swagger annotations (clean DTOs)
- ❌ No JPA annotations (DTOs are separate from entities)

### 4. OpenApiConfig

Location: `src/main/java/com/example/reservation/config/OpenApiConfig.java`

**Purpose:** Loads `openapi.yml` at runtime and exposes it via Swagger UI.

```java
@Bean
public OpenAPI customOpenAPI() throws IOException {
    ClassPathResource resource = new ClassPathResource("openapi.yml");
    String yamlContent = resource.getContentAsString(StandardCharsets.UTF_8);
    return new OpenAPIV3Parser().readContents(yamlContent, null, parseOptions).getOpenAPI();
}
```

This makes the API documentation available at:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

## Usage

### Generating DTOs

DTOs are automatically generated during the Maven build process:

```bash
# Generate DTOs only
mvn generate-sources

# Full build (includes DTO generation)
mvn clean install

# Docker build (includes DTO generation)
docker compose build backend
```

The generated DTOs will be in:
```
target/generated-sources/openapi/src/main/java/com/example/reservation/dto/generated/
```

### Viewing Generated DTOs in IDE

Most IDEs automatically detect generated sources. If not:

**IntelliJ IDEA:**
1. Right-click on `target/generated-sources/openapi/src/main/java`
2. Select "Mark Directory as" → "Generated Sources Root"

**Eclipse:**
1. Right-click on project → "Properties"
2. "Java Build Path" → "Source" tab
3. Add folder: `target/generated-sources/openapi/src/main/java`

### Using Generated DTOs (Future Migration)

**Current state:** Controllers use existing DTOs in `com.example.reservation.dto`.

**Future state:** Controllers will use generated DTOs from `com.example.reservation.dto.generated`.

**Migration example:**

Before:
```java
@PostMapping
public ResponseEntity<PropertyDto.Response> createProperty(
    @Valid @RequestBody PropertyDto.CreateRequest request
) { ... }
```

After:
```java
@PostMapping
public ResponseEntity<PropertyResponse> createProperty(
    @Valid @RequestBody PropertyCreateRequest request
) { ... }
```

**Mapping strategy:**
```java
// Option 1: Manual mapping
PropertyResponse toResponse(Property entity) {
    return new PropertyResponse()
        .id(entity.getId())
        .title(entity.getTitle())
        // ...
}

// Option 2: MapStruct
@Mapper
interface PropertyMapper {
    PropertyResponse toResponse(Property entity);
    Property toEntity(PropertyCreateRequest request);
}
```

## Maintaining openapi.yml

### Adding a New Endpoint

1. **Define the path and operation:**
```yaml
paths:
  /api/properties/{id}/photos:
    post:
      tags:
        - Properties
      summary: Upload property photo
      operationId: uploadPropertyPhoto
      parameters:
        - $ref: '#/components/parameters/PropertyId'
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              $ref: '#/components/schemas/PropertyPhotoUploadRequest'
      responses:
        '201':
          description: Photo uploaded
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PropertyPhotoResponse'
```

2. **Define the schemas:**
```yaml
components:
  schemas:
    PropertyPhotoUploadRequest:
      type: object
      required:
        - file
      properties:
        file:
          type: string
          format: binary
          description: Photo file
        caption:
          type: string
          maxLength: 255
          description: Photo caption

    PropertyPhotoResponse:
      type: object
      required:
        - id
        - url
      properties:
        id:
          type: string
          format: uuid
        url:
          type: string
          description: Photo URL
        caption:
          type: string
```

3. **Regenerate DTOs:**
```bash
mvn clean generate-sources
```

4. **Implement the controller method** using the generated DTOs.

### Adding Validation Rules

Validation is defined in the OpenAPI schema:

```yaml
PropertyCreateRequest:
  type: object
  required:
    - title
    - pricePerNight
  properties:
    title:
      type: string
      maxLength: 120
      minLength: 3
      pattern: '^[a-zA-Z0-9\s]+$'
    pricePerNight:
      type: number
      format: decimal
      minimum: 0.01
      maximum: 99999.99
```

This generates:
```java
@NotNull
@Size(min = 3, max = 120)
@Pattern(regexp = "^[a-zA-Z0-9\\s]+$")
private String title;

@NotNull
@Valid
@DecimalMin("0.01")
@DecimalMax("99999.99")
private BigDecimal pricePerNight;
```

### Pagination

All list endpoints support pagination with these parameters:
- `unpaged` (boolean) - If true, return all results
- `page` (integer) - Page number (0-indexed)
- `size` (integer) - Page size (default: 20, max: 100)
- `sort` (string) - Sort field and direction (e.g., "createdAt,desc")

**OpenAPI definition:**
```yaml
parameters:
  - $ref: '#/components/parameters/Unpaged'
  - $ref: '#/components/parameters/Page'
  - $ref: '#/components/parameters/Size'
  - $ref: '#/components/parameters/Sort'

responses:
  '200':
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/PageResponse_PropertyListResponse'
```

**Behavior:**
- When `unpaged=true`: returns all results, pagination params ignored
- When `unpaged=false` (default): returns paginated results

## Testing the API

### Via Swagger UI

1. Start the application:
```bash
docker compose up -d
```

2. Open Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

3. Obtain JWT token:
```bash
curl -X POST 'http://localhost:8081/realms/reservation/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=reservation-test&username=testuser&password=testuser'
```

4. Click "Authorize" in Swagger UI and paste the token.

5. Try any endpoint directly from the UI.

### Via curl

```bash
# Get all properties (paginated)
curl http://localhost:8080/api/properties

# Get all properties (unpaged)
curl http://localhost:8080/api/properties?unpaged=true

# Get properties with pagination
curl "http://localhost:8080/api/properties?page=0&size=10&sort=createdAt,desc"

# Create a property (requires JWT)
curl -X POST http://localhost:8080/api/properties \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Property",
    "description": "A test property",
    "city": "Paris",
    "pricePerNight": 150.00
  }'
```

## Validation

The generated DTOs include full Jakarta validation support:

### Request Validation

```java
@PostMapping
public ResponseEntity<PropertyResponse> createProperty(
    @Valid @RequestBody PropertyCreateRequest request  // @Valid triggers validation
) { ... }
```

If validation fails, Spring Boot returns a `400 Bad Request` with `ProblemDetail`:
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "title: must not be blank; pricePerNight: must be greater than 0",
  "instance": "/api/properties"
}
```

### Validation Annotations Used

- `@NotNull` - Field cannot be null
- `@NotBlank` - String cannot be null or empty
- `@Size(min=X, max=Y)` - String/Collection size constraints
- `@DecimalMin(value)` - Minimum numeric value
- `@DecimalMax(value)` - Maximum numeric value
- `@Email` - Valid email format
- `@Pattern(regexp)` - Regex pattern matching
- `@Valid` - Cascading validation for nested objects

## FAQ

### Why generate DTOs instead of writing them manually?

**Benefits:**
1. **Single source of truth** - API contract defined once in openapi.yml
2. **Consistency** - All DTOs follow the same structure and conventions
3. **Documentation** - API docs always match implementation
4. **Type safety** - Changes to openapi.yml require recompilation
5. **Validation** - Validation rules defined once, applied everywhere
6. **Maintenance** - Less code to maintain manually

### Can I customize generated DTOs?

**No** - Generated DTOs should not be modified directly as they're regenerated on each build.

**Instead:**
1. Modify `openapi.yml` to change the schema
2. Create mapper classes to transform between generated DTOs and domain entities
3. Use inheritance if you need to add custom methods (though not recommended)

### What about entities?

**Separation of concerns:**
- **Entities** (`com.example.reservation.domain`) - JPA entities, database mapping
- **DTOs** (`com.example.reservation.dto.generated`) - API contract, JSON serialization

**Entities and DTOs should remain separate:**
- Entities change based on database requirements
- DTOs change based on API contract requirements
- Use mappers to transform between them

### What happens to existing DTOs?

The existing DTOs in `com.example.reservation.dto` package can:
1. **Be removed** after migration to generated DTOs
2. **Coexist** during transition period
3. **Be used for custom DTOs** not defined in OpenAPI (internal use)

### Do I need to commit generated files?

**No** - Generated files in `target/generated-sources/` should NOT be committed to git.

They're automatically regenerated during:
- `mvn generate-sources`
- `mvn compile`
- `mvn install`
- Docker builds

### How do I handle breaking changes?

1. **Backward compatibility:**
   - Add new fields as optional
   - Never remove fields without deprecation
   - Version your API if needed (`/api/v2/...`)

2. **Non-breaking changes:**
   - Add new optional fields
   - Add new endpoints
   - Add new enum values (at the end)

3. **Breaking changes:**
   - Update openapi.yml
   - Regenerate DTOs
   - Update controllers
   - Update tests
   - Update documentation

## Best Practices

### 1. Keep openapi.yml DRY

Use `$ref` for reusable components:
```yaml
# Bad - Repetition
PropertyCreateRequest:
  properties:
    title:
      type: string
      maxLength: 120
PropertyUpdateRequest:
  properties:
    title:
      type: string
      maxLength: 120

# Good - Reusable
components:
  schemas:
    PropertyTitle:
      type: string
      maxLength: 120

    PropertyCreateRequest:
      properties:
        title:
          $ref: '#/components/schemas/PropertyTitle'
```

### 2. Document Everything

```yaml
PropertyResponse:
  description: Détails complets d'une propriété
  required:
    - id
  properties:
    id:
      type: string
      format: uuid
      description: Identifiant unique de la propriété
      example: 550e8400-e29b-41d4-a716-446655440000
```

### 3. Use Consistent Naming

- **Requests:** `{Entity}{Action}Request` (e.g., `PropertyCreateRequest`)
- **Responses:** `{Entity}Response` (e.g., `PropertyResponse`)
- **Lists:** `{Entity}ListResponse` (e.g., `PropertyListResponse`)
- **Pages:** `PageResponse_{Entity}ListResponse`

### 4. Define Validation at the Schema Level

```yaml
email:
  type: string
  format: email
  maxLength: 255
  description: Email de l'utilisateur
```

Generates:
```java
@Email
@Size(max = 255)
private String email;
```

### 5. Use Enums for Fixed Values

```yaml
PropertyStatus:
  type: string
  enum:
    - ACTIVE
    - INACTIVE
  description: Statut d'une propriété
```

### 6. Document Error Responses

```yaml
responses:
  '400':
    description: Données invalides
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/ProblemDetail'
```

## Troubleshooting

### DTOs not generated

```bash
# Clean and regenerate
mvn clean generate-sources

# Check for errors in openapi.yml
mvn validate
```

### IDE doesn't recognize generated classes

1. Reimport Maven project
2. Mark `target/generated-sources/openapi/src/main/java` as "Generated Sources Root"
3. Rebuild project

### Swagger UI shows old spec

```bash
# Clear Spring Boot cache
mvn clean
docker compose down
docker compose up --build
```

### Validation not working

Ensure `@Valid` annotation is present:
```java
public ResponseEntity<PropertyResponse> createProperty(
    @Valid @RequestBody PropertyCreateRequest request  // ← Must have @Valid
) { ... }
```

### Compilation errors after regeneration

1. Check if openapi.yml has breaking changes
2. Update controller method signatures
3. Update mappers if using generated DTOs

## Next Steps

1. **Review generated DTOs** - Check `target/generated-sources/openapi/`
2. **Test Swagger UI** - Visit http://localhost:8080/swagger-ui.html
3. **Plan migration** - Decide when to migrate from manual to generated DTOs
4. **Update controllers** - Gradually replace existing DTOs with generated ones
5. **Create mappers** - Use MapStruct or manual mappers for entity ↔ DTO conversion

## Resources

- [OpenAPI Specification](https://swagger.io/specification/)
- [OpenAPI Generator Documentation](https://openapi-generator.tech/)
- [springdoc-openapi Documentation](https://springdoc.org/)
- [Jakarta Validation Specification](https://jakarta.ee/specifications/bean-validation/)
