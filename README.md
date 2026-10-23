## Live Deployment

- **Swagger UI:** https://document-storage-freightfox.onrender.com/swagger-ui/index.html
- **Health check:** https://document-storage-freightfox.onrender.com/actuator/health
- **OpenAPI specification:** https://document-storage-freightfox.onrender.com/v3/api-docs

The application is deployed on Render using Docker and Java 21, with Amazon S3 as the document storage backend.

### Deployment verification

The following integration checks were performed against the deployed API:

- Search documents in S3
- Upload a synthetic test document
- Generate and use a presigned download URL
- Verify search results are scoped to the requested user prefix

The automated test suite also passes locally with `mvn clean verify`.

**Note:** The assignment API does not implement authentication. The username in the URL selects a storage namespace; it does not authenticate the caller. Use synthetic data only. The free hosting service may sleep when idle, so the first request after inactivity may take longer to respond.