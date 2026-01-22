# Swagger/OpenAPI Documentation

## Overview
The Luna API includes interactive API documentation using Swagger UI (OpenAPI 3.0).

## Accessing Swagger UI

Once the application is running, you can access the Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

Or the alternative path:
```
http://localhost:8080/swagger-ui/index.html
```

## OpenAPI JSON/YAML

The raw OpenAPI specification is available at:

```
http://localhost:8080/api-docs
```

For YAML format:
```
http://localhost:8080/api-docs.yaml
```

## Authentication in Swagger

The API uses JWT Bearer token authentication. To test authenticated endpoints:

1. First, register a new user via `/api/auth/register`
2. Verify your email via `/api/auth/verify-email`
3. Login via `/api/auth/login` to get your access token
4. Click the "Authorize" button at the top of Swagger UI
5. Enter your token in the format: `Bearer <your-access-token>`
6. Click "Authorize" and then "Close"
7. Now you can test authenticated endpoints

## Features

- Interactive API testing
- Request/response examples
- Schema definitions
- JWT authentication support
- Organized by tags (Authentication, Health, etc.)
- Rate limiting information in endpoint descriptions

## Configuration

Swagger configuration can be modified in:
- `src/main/resources/application.yml` - Basic settings
- `src/main/java/com/luna/config/OpenApiConfig.java` - Advanced customization
