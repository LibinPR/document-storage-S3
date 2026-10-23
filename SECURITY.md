# Security Considerations

## Input Validation

The upload endpoint validates:
- Usernames against a restricted alphanumeric, underscore, and hyphen pattern.
- Empty uploads and a maximum file size of 10 MB.
- Filenames for length, path separators, control characters, and invalid values.
- File extensions against an explicit allowlist: PDF, TXT, Markdown, DOCX, and CSV.
- Declared MIME types against the supported types for each extension when a MIME type is provided.

These checks reduce accidental misuse but do not prove that a file's contents match its extension or MIME type.

## S3 Access

- The S3 bucket should remain private.
- Runtime IAM permissions should be restricted to the application's required bucket and `users/*` object prefix.
- Avoid granting deletion permissions unless the application requires deletion.
- Prefer managed or platform-provided secret storage for AWS credentials.

## Download URLs

Presigned download URLs are bearer credentials. Anyone holding an unexpired URL may be able to access the associated object. Keep their lifetime short and avoid exposing them in logs or public documentation.

## Authentication Limitation

The API does not implement authentication, as specified by the assignment. A username in the URL selects a storage prefix but does not establish the caller's identity. Prefix scoping is not a substitute for authorization.

The public demo should contain only synthetic data.

## Known Limitations

- The application does not perform malware scanning.
- MIME type and extension checks do not inspect the file's actual contents.
- No authentication, authorization, or per-client upload rate limiting is implemented.
- Public deployment requires careful monitoring of S3 usage and costs.