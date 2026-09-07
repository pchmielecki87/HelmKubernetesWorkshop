# Shop backend

Spring Boot service used by the ArgoCD example.

- PostgreSQL stores the product catalogue in the `products` table.
- Redis stores shopping carts as hashes under `cart:<cartId>`.
- The health endpoint is available at `/actuator/health`.

Build the image for Docker Desktop Kubernetes from the repository root:

```bash
docker build -t shop-backend:dev ArgoCD/app
```

Example API calls after the service is exposed:

```bash
curl http://localhost:8080/api/products
curl -X POST http://localhost:8080/api/carts/alice/items \
  -H 'Content-Type: application/json' \
  -d '{"productId":1,"quantity":2}'
curl http://localhost:8080/api/carts/alice
```
