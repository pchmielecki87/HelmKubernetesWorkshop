# Labs

## Section: Multi-Environment Deployments with Kustomize & ApplicationSets

### Step 15: Create the Test Environment Overlay

Extend the existing baseline manifest set by creating a lightweight environment overlay for `test` without duplicating base definitions.

Create the `test` overlay directory and copy the base `kustomization.yaml` structure:

```bash
mkdir -p environments/test
cp environments/dev/kustomization.yaml environments/test/
```

Configure the test overlay parameters:
- Update environments/test/kustomization.yaml to target the shop-test namespace.
- Apply environmental overrides (such as adjusting replica counts or resource limits) while referencing the shared base manifests.

Validate the Kustomize rendering and test cluster dry-run locally:

```bash
kubectl kustomize environments/test > /tmp/test.yaml
kubectl apply --dry-run=client -f /tmp/test.yaml
kubectl apply --dry-run=server -f /tmp/test.yaml
```

Expected result:
- The generated manifest bundle in /tmp/test.yaml renders without errors.
- (Client) All resources explicitly target the shop-test namespace while preserving base configuration logic.
- (Server) `Error from server (NotFound): error when creating "/tmp/test.yaml": namespaces "shop-test" not found`.

NOTE: Eventually we can create namespace manually:

```bash
kubectl create namespace shop-test
kubectl apply --dry-run=server -f /tmp/test.yaml
```

but ArgoCD can do it for us:

```yaml
syncPolicy:
  automated:
    prune: true
    selfHeal: true
  syncOptions:
    - CreateNamespace=true
```

### 16: Automate Environment Provisioning with ApplicationSet

Use an ArgoCD ApplicationSet controller with a list generator to dynamically generate and manage Application resources across dev and test environments.

Create or update applicationset.yaml using a list generator to define environment mappings:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: shop-stack
  namespace: argocd
spec:
  generators:
  - list:
      elements:
      - env: dev
        ns: shop-dev
      - env: test
        ns: shop-test
  template:
    metadata:
      name: shop-{{env}}
    spec:
      project: default
      source:
        repoURL: 'https://github.com/YOUR_USERNAME/YOUR_REPO.git'
        targetRevision: HEAD
        path: ArgoCD/environments/{{env}}
      destination:
        server: 'https://kubernetes.default.svc'
        namespace: '{{ns}}'
      syncPolicy:
        automated:
          prune: true
          selfHeal: true
        syncOptions:
        - CreateNamespace=true
```

NOTE: COMMENT ALL IN ROOT-APPLICATION.

Apply the ApplicationSet manifest to the cluster:

```bash
kubectl apply -f applicationset.yaml
```

In case of failure `no matches for kind "ApplicationSet" in version "argoproj.io/v1alpha1"
ensure CRDs are installed` install the Custom Resource Definition (CRD):

```bash
kubectl create -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/crds/applicationset-crd.yaml
```

and re-run `kubectl apply -f applicationset.yaml`.

Verify that the generator created individual Application resources for both environments:

```bash
kubectl get applications -n argocd
```

Expected result: The ApplicationSet controller generates two distinct Application objects (shop-dev and shop-test) pointing to their respective paths and target namespaces.

NOTE: if there is a problem with login to ArgoCD UI get once more the password:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
```

NOTE 2: If in ArgoCD app seems stuck in sync process:

```bash
kubectl patch app shop-stack-prod -n argocd --type json -p='[{"op": "remove", "path": "/status/operationState"}]'
```

### 17: Validate Multi-Env Health & Enforce AppProject Boundaries

Verify application deployment status across environments and validate that ArgoCD AppProject guardrails block deployments to unauthorized namespaces.
Inspect application mapping and parameter propagation:

```bash
kubectl get applications -n argocd \
  -o custom-columns=NAME:.metadata.name,PATH:.spec.source.path,NS:.spec.destination.namespace
```

Expected result: Output displays shop-dev targeting ArgoCD/environments/dev -> shop-dev, and shop-test targeting ArgoCD/environments/test -> shop-test.

Confirm synchronization and health status for all generated environments in ArgoCD UI.
