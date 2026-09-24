# Labs

## Section: Monitoring & Troubleshooting

### Prerequisites: Quick Prometheus & Grafana Setup (Docker Desktop)

Install the Prometheus & Grafana monitoring stack via Helm optimized for Docker Desktop resource limits.

1. Add the Helm repository and install `kube-prometheus-stack`:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set alertmanager.enabled=false \
  --set prometheus.prometheusSpec.resources.requests.memory=256Mi \
  --set prometheus.prometheusSpec.resources.limits.memory=512Mi
```

Decode secret:

```bash
kubectl get secret --namespace monitoring monitoring-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

Access the Grafana Dashboard locally:

```bash
kubectl port-forward svc/monitoring-grafana 3000:80 -n monitoring
```

URL: http://localhost:3000
Username: admin
Password: generated secret

### 12: Trigger an Deployment Failure via Git

Simulate a failed deployment scenario by introducing an invalid container image tag into your Git repository so ArgoCD treats it as the desired state.
Modify the backend image tag in your Kustomize overlay (environments/dev/kustomization.yaml):

```yaml
images:
  - name: wiremock/wiremock
    newName: wiremock/wiremock
    newTag: dev-does-not-exist
```

Commit and push the broken configuration to Git:

```bash
git add environments/dev/kustomization.yaml
git commit -m "lab: invalid backend tag"
git push origin main
```

Observe the deployment status in your local cluster:

```bash
kubectl get application shop-stack-dev -n argocd -w
kubectl get pods -n shop-dev -w
```

Expected result: The Application state in ArgoCD transitions away from Healthy, and the newly created backend Pod fails to reach the Ready state.

### 13: Diagnose the Root Cause

Perform a structured root-cause analysis by inspecting ArgoCD, the Deployment status, and the Pod events to locate the issue.
Inspect the overall application health in ArgoCD:

```bash
kubectl describe application shop-stack-dev -n argocd
```

Expected result: Look for Degraded or Progressing health statuses, sync condition errors, or failed operations.
Check the Deployment rollout status and list running Pods:

```bash
kubectl rollout status deploy/shop-backend -n shop-dev --timeout=60s
kubectl get pods -n shop-dev
```

Expected result: The rollout exceeds the timeout threshold, and the new Pod shows an ImagePullBackOff or ErrImagePull status.

Stream application container logs to verify if the process fails at startup or fails to pull:

```bash
kubectl logs -l app=shop-backend -n shop-dev --all-containers --tail=50
```

Inspect Kubernetes events to confirm the exact failure reason:

```bash
kubectl describe pod -l app=shop-backend -n shop-dev
kubectl get events -n shop-dev --sort-by='.lastTimestamp'
```

Inspect node-level system journal / container daemon logs (simulated on Docker Desktop node via kubectl node-shell or debug pod):

```bash
# 1. Create debug pod in background
NODE_NAME=$(kubectl get nodes -o jsonpath='{.items[0].metadata.name}')
kubectl debug node/$NODE_NAME -n default --image=alpine --profile=sysadmin -it -- chroot /host journalctl -u containerd -n 50

# 2. Read logs from created pod
kubectl logs pod/node-debugger-desktop-control-plane-<number> -n default

# 3. Cleanup after debugging
kubectl get pods -n default | node-debugger
kubectl delete pod node-debugger-<xyz> -n default
```

Expected result:

- kubectl events confirms a Failed / PullImage event stating that the image tag wiremock/wiremock:dev-does-not-exist was not found.
- journalctl / node logs confirm container pull failures at the runtime layer (containerd).

### 14: Repair and Verify Service Recovery

Fix the desired state in Git using GitOps practices (do not edit the Deployment directly using kubectl). Auto-sync will deploy the corrected revision and recover the service.
Revert the breaking commit in Git (or fix the tag manually in kustomization.yaml):

```bash
git revert HEAD --no-edit
git push origin main
```

Wait for ArgoCD to detect the fix and complete the rollout:

```bash
kubectl rollout status deploy/shop-backend -n shop-dev
```

Expected result: ArgoCD status returns to Synced and Healthy, with the rollout successfully completed.
Verify service health in ArgoCD UI portal. Expected result: All Pods display 1/1 Running.
