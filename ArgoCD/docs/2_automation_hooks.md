## Section: Automation and Hooks

### 6: Simulate drift and confirm self-healing

Check the current desired state in Git:

```bash
kubectl get deployment shop-backend -n shop-dev -o jsonpath='{.spec.replicas}'
```

Manually change the live Deployment replica count to create drift:

```bash
kubectl scale deployment/shop-backend -n shop-dev --replicas=1
kubectl get application shop-stack-dev -n argocd
```

ArgoCD should detect the drift and restore the desired value from Git automatically. You can verify it with:

```bash
kubectl get deployment shop-backend -n shop-dev -o jsonpath='{.spec.replicas}'
kubectl get application shop-stack-dev -n argocd -o yaml | grep -E 'Status:|Health:|Operation'
```

Expected result: the replica count returns to the Git-defined value (for this repo: `2`), and the application status becomes `Synced` and `Healthy` again.

### 7: Practice prune behavior

This exercise shows how ArgoCD removes resources that no longer exist in Git when `prune: true` is enabled.

1. Add a temporary `ConfigMap` to the Git-managed manifests first, then apply it:

```bash
cat <<'EOF' > base/temp-prune-check.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: temp-prune-check
  namespace: shop-dev
  labels:
    app: drift-demo
data:
  note: "This ConfigMap is managed by Git and should be pruned when removed from Git"
EOF
```

Update the base Kustomization to include the file:

```yaml
resources:
  - java-app.yaml
  - postgres.yaml
  - redis.yaml
  - temp-prune-check.yaml
```

Then sync:

```bash
kubectl apply -k environments/dev
kubectl get configmap -n shop-dev
kubectl get application shop-stack-dev -n argocd
```

2. Remove the file from Git and from the Kustomization, then trigger a sync:

```bash
rm base/temp-prune-check.yaml
```

And update `base/kustomization.yaml` to remove the entry from `resources`. Then push the changes to Git repo.

Then refresh ArgoCD:

```bash
kubectl get application shop-stack-dev -n argocd
kubectl get configmap -n shop-dev
```

Expected result: the temporary `ConfigMap` disappears because ArgoCD prunes resources no longer declared in Git.

3. Optional extension: compare with the `kubectl get application ... -o yaml` output and verify the `syncPolicy.automated.prune: true` setting from `root-application.yaml`.

### 8: Practice ArgoCD PreSync Hooks (Database Migration Job)

This exercise demonstrates how ArgoCD uses **Resource Hooks** to run a database migration `Job` during the `PreSync` phase—executing and verifying it before sync applies the main application resources (`Deployment`).

1. Add a PreSync `Job` manifest for the database migration:

```bash
cat <<'EOF' > base/db-migration-job.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: db-migration-job
  annotations:
    argocd.argoproj.io/hook: PreSync
    argocd.argoproj.io/hook-delete-policy: HookSucceeded
spec:
  template:
    spec:
      containers:
      - name: db-migrator
        image: postgres:15-alpine
        command: ["sh", "-c", "echo 'Running PreSync DB Schema Migration...' && sleep 5"]
      restartPolicy: Never
  backoffLimit: 1
EOF
```

Update the base Kustomization to include the migration job:

```yaml
resources:
  - java-app.yaml
  - postgres.yaml
  - redis.yaml
  - db-migration-job.yaml
```

Commit and push the changes to your Git repository. Trigger a sync and observe the execution order in ArgoCD:

```bash
kubectl get pods -n shop-dev -w
```

Expected result: ArgoCD first creates and waits for db-migration-job to complete successfully during the PreSync phase. Only after the job finishes will ArgoCD proceed to synchronize the main application stack (shop-backend, postgres, redis). Once finished, the job pod is automatically cleaned up per the HookSucceeded delete policy.
