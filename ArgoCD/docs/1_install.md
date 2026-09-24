## Section: Initinal Config and App Management

### 1: Install ArgoCD

Run the commands from the repository root:

```bash
kubectl get namespace argocd >/dev/null 2>&1 || kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl rollout status deployment/argocd-server -n argocd --timeout=180s
kubectl get all -n argocd
```

### 2: Build and publish the backend image

```bash
docker build -t shop-backend:dev ArgoCD/app
docker tag shop-backend:dev ghcr.io/pchmielecki87/shop-backend:dev
docker push ghcr.io/pchmielecki87/shop-backend:dev
```

The GHCR package must be public, or the cluster must have an image pull Secret.

To verify if image is in place navigate to https://github.com/pchmielecki87?tab=packages or use CLI command:

```bash
docker buildx imagetools inspect ghcr.io/pchmielecki87/shop-backend:dev
```

### 3: Apply and inspect the ArgoCD Application

```bash
kubectl apply -f ArgoCD/root-application.yaml
kubectl get application shop-stack-dev -n argocd
kubectl get application shop-stack-dev -n argocd -w
```

Wait until the application reports `Synced` and `Healthy`, then verify the workloads:

```bash
kubectl get pods -n shop-dev
kubectl rollout status deployment/shop-backend -n shop-dev --timeout=180s
```

### 4: Test the API locally

Check the app health:

```bash
kubectl port-forward svc/shop-backend-service -n shop-dev 8080:8080
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/products
```

Add item to cart:

```bash
curl -X POST http://localhost:8080/api/carts/alice/items \
  -H 'Content-Type: application/json' \
  -d '{"productId":1,"quantity":2}'
```

Check cart (3 options):

```bash
curl -X GET http://localhost:8080/api/carts/alice
curl -s http://localhost:8080/api/carts/alice | jq .
kubectl exec -it deployment/redis -n shop-dev -- redis-cli HGETALL cart:alice
```

Delete item from cart:

```bash
curl -X DELETE http://localhost:8080/api/carts/alice
```

### 5: Log in to the ArgoCD Web UI

Get the initial admin password and forward the ArgoCD server port locally:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
kubectl port-forward svc/argocd-server -n argocd 8088:443
```

Then open:

```text
https://localhost:8088
```

Login with:

```text
username: admin
password: <password-from-command-above>
```

> The browser may show a certificate warning because ArgoCD uses a self-signed certificate by default. Accept the warning and continue.
