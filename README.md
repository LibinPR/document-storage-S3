# User Document Storage Service

A REST API built with Java 21 and Spring Boot 3.5 for searching, uploading, and downloading user documents stored in a private Amazon S3 bucket.

## Features

- Search documents by filename, with case-insensitive partial matching.
- Isolate document searches by user-specific S3 prefixes.
- Cursor-based pagination for search results.
- Generate short-lived presigned S3 download URLs.
- Upload files up to 10 MB.
- Validate usernames, empty files, and upload size limits.
- Interactive API documentation with Swagger UI.
- Automated unit and controller tests.

## Technology Stack

- Java 21
- Spring Boot 3.5.6
- Maven
- Amazon S3 and AWS SDK for Java 2.x
- Spring MVC
- Springdoc OpenAPI / Swagger UI
- JUnit 5, Mockito, and Spring Boot Test

## Architecture

The application separates HTTP request handling, business logic, and storage operations.

- **Controller:** exposes the REST endpoints.
- **Search service:** handles filename matching and cursor-based pagination.
- **Download service:** validates document access and generates presigned URLs.
- **Upload service:** validates uploaded files and sends them to storage.
- **Storage abstraction:** separates application logic from the S3 implementation.
- **S3 storage implementation:** performs object listing, existence checks, uploads, and related S3 operations.

### S3 object layout

Documents use the following key format:

`users/{userName}/{fileName}`

For example:

`users/libin/test-doc.txt`

The application uses a single S3 bucket. The bucket should remain private, and application access should be granted through appropriately scoped AWS IAM permissions.

**Important:** This assignment intentionally does not implement authentication. A username in the URL selects a storage namespace; it does not prove the caller owns that user identity. Use only synthetic or demo data when exposing this service publicly.

## Prerequisites

- Java 21
- Maven
- An AWS account and an S3 bucket in a supported region
- AWS credentials with the required S3 permissions

## Configuration

The application reads configuration from environment variables.

| Variable | Purpose | Default |
|---|---|---|
| `PORT` | HTTP server port | `8080` |
| `AWS_REGION` | AWS region | `ap-south-1` |
| `S3_BUCKET_NAME` | S3 bucket name | Empty; must be configured |
| `DOCUMENT_DEFAULT_PAGE_SIZE` | Default search page size | `20` |
| `DOCUMENT_MAX_PAGE_SIZE` | Maximum search page size | `100` |
| `DOCUMENT_CURSOR_SIGNING_SECRET` | Secret used to sign pagination cursors | Empty; must be configured |
| `DOCUMENT_PRESIGNED_URL_TTL_SECONDS` | Download URL lifetime | `300` |

Set the required environment variables before starting the application. Use a securely generated cursor-signing secret of at least 32 bytes. Never commit AWS credentials, session tokens, or real secrets to source control.

The application uses the AWS SDK default credentials provider chain. For local development, use an appropriately configured AWS profile. For AWS-hosted deployment, prefer an IAM role attached to the compute service instead of relying on a temporary local login session.

## Run Locally

From the project root:

```bash
mvn clean verify
mvn spring-boot:run
```

The application starts on port `8080` by default.

### API documentation

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health endpoint: `http://localhost:8080/actuator/health`

## API Reference

### 1. Search documents

`GET /api/v1/users/{userName}/documents`

Optional query parameters:

- `query`: filename substring to search for.
- `pageSize`: number of results requested, from 1 to 100.
- `cursor`: opaque cursor returned by a previous response.

Example:

```bash
curl "http://localhost:8080/api/v1/users/libin/documents?query=invoice&pageSize=20"
```

The response contains a `documents` array. Each document includes its filename, S3 key, size in bytes, and last-modified timestamp. When more results are available, the response includes `nextCursor`; pass it as `cursor` to retrieve the next page.

### 2. Upload a document

`POST /api/v1/users/{userName}/documents`

Content type: `multipart/form-data`

Required field: `file`

Example:

```bash
curl -X POST \
  "http://localhost:8080/api/v1/users/libin/documents" \
  -F "file=@test-doc.txt;type=text/plain"
```

A successful upload returns HTTP `201 Created` with a message, filename, and S3 key.

The maximum upload size is 10 MB. Empty files and invalid usernames are rejected.

### 3. Generate a download URL

`GET /api/v1/users/{userName}/documents/{fileName}/download`

Example:

```bash
curl \
  "http://localhost:8080/api/v1/users/libin/documents/test-doc.txt/download"
```

A successful response contains a `downloadUrl` field. Use the returned URL to download the object directly from S3. The URL expires after the configured lifetime, which defaults to 300 seconds.

Treat presigned URLs as bearer credentials. Do not publish or log them unnecessarily.

## Error Handling

The API uses appropriate HTTP status codes for common failures.

| Status | Meaning |
|---|---|
| `200 OK` | Search or download URL generation succeeded |
| `201 Created` | Document uploaded successfully |
| `400 Bad Request` | Invalid input, username, or page size |
| `404 Not Found` | Requested document does not exist |
| `413 Payload Too Large` | Upload exceeds the configured size limit |
| `500 Internal Server Error` | Unexpected application or configuration failure |

## AWS IAM Permissions

The runtime identity needs only the S3 permissions required by the application:

- `s3:ListBucket` on the configured bucket, restricted to the application's `users/*` prefix where appropriate.
- `s3:GetObject` on the required `users/*` objects.
- `s3:PutObject` on the required `users/*` objects when uploads are enabled.

Configure the bucket name and region for the target environment. Keep the bucket private and never commit access keys or secret credentials.

## Tests

Run the full verification suite:

```bash
mvn clean verify
```

The current suite contains 10 tests covering controller behavior, filename matching, input validation, cursor tampering, and pagination.

Manual integration checks have also verified upload, search, and presigned download against the configured S3 bucket, as well as tested user-folder search isolation and upload-size rejection.

## Deployment Status

Deployment instructions and a live API URL will be added after deployment has been completed and verified. Local Swagger and health URLs are not public deployment endpoints.
