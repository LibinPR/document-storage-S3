# System Design — User Document Storage Service

## 1. Problem Statement

Build a REST API that searches and downloads files stored in Amazon S3, scoped to a user-specific storage namespace. Upload support is an additional capability.

## 2. Requirements

### Functional
- Search files by filename substring.
- Scope each search to the requested user's S3 prefix.
- Return paginated results.
- Generate presigned URLs for downloads.
- Upload files to the corresponding user prefix.

### Non-functional
- Avoid unnecessary S3 API calls.
- Keep storage operations behind an abstraction.
- Validate inputs and return appropriate HTTP status codes.
- Support automated testing and API exploration through Swagger.

## 3. Architecture

The application uses a layered Spring Boot architecture:

- **Controller:** HTTP endpoints, request parameters, and response status codes.
- **Search service:** filename filtering, result pagination, and cursor handling.
- **Download service:** document existence checks and presigned URL generation.
- **Upload service:** upload input validation and object-key construction.
- **Storage abstraction:** isolates application logic from the storage provider.
- **S3 adapter:** performs object listing, retrieval-related checks, uploads, and other S3 operations.

## 4. Storage Layout

All documents are stored in one private S3 bucket.

Object key convention:

`users/{userName}/{fileName}`

Example:

`users/sandy/logistics.pdf`

This convention allows searches to be restricted by prefix without a separate database table for user folders.

## 5. Search Flow

1. Receive the username, optional filename query, page size, and cursor.
2. Validate the request parameters.
3. Construct the S3 prefix for the requested user.
4. List objects under that prefix using S3 pagination.
5. Filter filenames according to the search term.
6. Return the page of results and a signed continuation cursor when more results are available.

The implementation should avoid loading the entire bucket into memory. Pagination bounds the amount of data returned to each caller.

## 6. Download Flow

1. Receive the username and filename.
2. Construct the expected S3 object key.
3. Verify that the object exists within the user's prefix.
4. Generate a short-lived presigned S3 URL.
5. Return the URL to the caller.

The client downloads directly from S3 rather than routing the entire file through the application server.

## 7. Upload Flow

1. Accept a multipart file.
2. Validate the username, filename, and file size.
3. Construct the S3 object key.
4. Upload the object using the storage abstraction.
5. Return HTTP 201 Created with the filename and object key.

## 8. Pagination and Cursor Integrity

The API uses a continuation cursor to resume searches across pages. The cursor is signed using an HMAC-based mechanism so modifications can be detected.

The cursor is opaque to API consumers. The page size is bounded to prevent excessively large responses.

## 9. Security and Trade-offs

- The S3 bucket remains private.
- IAM permissions are scoped to the required bucket and user prefix.
- Presigned URLs are short-lived bearer credentials.
- The API intentionally does not implement authentication, as specified by the assignment.
- A username in the URL selects a namespace; it does not authenticate the caller.
- Public deployment should therefore use synthetic data only.

## 10. Limitations and Future Improvements

- Add authentication and authorization if the API is used beyond the assignment.
- Add upload rate limiting and quotas.
- Add structured request logging and metrics.
- Consider a metadata database if search requirements expand beyond filename matching.
- Add malware scanning if the service begins accepting untrusted files in a production environment.

These are possible future improvements, not claims about the current implementation.