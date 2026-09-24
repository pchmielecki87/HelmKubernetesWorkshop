# GitOps with ArgoCD Workshop
## 📋 Table of Contents
- [GitOps with ArgoCD Workshop](#gitops-with-argocd-workshop)
  - [📋 Table of Contents](#-table-of-contents)
- [ArgoCD GitOps Repository - Shop Application](#argocd-gitops-repository---shop-application)
  - [Folder Structure](#folder-structure)
  - [System Architecture](#system-architecture)
  - [What Is in the Manifests](#what-is-in-the-manifests)
- [Labs](#labs)
  - [Section: Initinal Config and App Management](#section-initinal-config-and-app-management)
    - [1: Install ArgoCD](#1-install-argocd)
    - [2: Build and publish the backend image](#2-build-and-publish-the-backend-image)
    - [3: Apply and inspect the ArgoCD Application](#3-apply-and-inspect-the-argocd-application)
    - [4: Test the API locally](#4-test-the-api-locally)
    - [5: Log in to the ArgoCD Web UI](#5-log-in-to-the-argocd-web-ui)
  - [Section: Automation and Hooks](#section-automation-and-hooks)
    - [6: Simulate drift and confirm self-healing](#6-simulate-drift-and-confirm-self-healing)
    - [7: Practice prune behavior](#7-practice-prune-behavior)
    - [8: Practice ArgoCD PreSync Hooks (Database Migration Job)](#8-practice-argocd-presync-hooks-database-migration-job)
  - [Section: IAM](#section-iam)
    - [9: Create developer user and RBAC permissions](#9-create-developer-user-and-rbac-permissions)
    - [10: Test Unauthorized Actions \& Policy Enforcement](#10-test-unauthorized-actions--policy-enforcement)
    - [11: Inspect Audit Logs and Security Events](#11-inspect-audit-logs-and-security-events)
  - [Section: Monitoring \& Troubleshooting](#section-monitoring--troubleshooting)
    - [Prerequisites: Quick Prometheus \& Grafana Setup (Docker Desktop)](#prerequisites-quick-prometheus--grafana-setup-docker-desktop)
    - [12: Trigger an Deployment Failure via Git](#12-trigger-an-deployment-failure-via-git)
    - [13: Diagnose the Root Cause](#13-diagnose-the-root-cause)
    - [14: Repair and Verify Service Recovery](#14-repair-and-verify-service-recovery)
  - [Section: Multi-Environment Deployments with Kustomize \& ApplicationSets](#section-multi-environment-deployments-with-kustomize--applicationsets)
    - [Step 15: Create the Test Environment Overlay](#step-15-create-the-test-environment-overlay)
    - [16: Automate Environment Provisioning with ApplicationSet](#16-automate-environment-provisioning-with-applicationset)
    - [17: Validate Multi-Env Health \& Enforce AppProject Boundaries](#17-validate-multi-env-health--enforce-appproject-boundaries)
  - [Section: CI/CD Pipeline \& Automated Promotion (GitLab CI)](#section-cicd-pipeline--automated-promotion-gitlab-ci)
    - [18: In GitLab CI build Java app and deploy it to Azure Container Apps](#18-in-gitlab-ci-build-java-app-and-deploy-it-to-azure-container-apps)
    - [19: Connect Azure Container Apps to GitOps Repository via Built-in Flux CD](#19-connect-azure-container-apps-to-gitops-repository-via-built-in-flux-cd)
      - [19.1 Architectural Overview (Managed Flux in ACA)](#191-architectural-overview-managed-flux-in-aca)
      - [19.2 GitOps Manifest Structure (manifests/containerapp.yaml)](#192-gitops-manifest-structure-manifestscontainerappyaml)
      - [19.3 End-to-End GitOps Deployment Workflow](#193-end-to-end-gitops-deployment-workflow)
- [Misc](#misc)
  - [Use Cases](#use-cases)
  - [Limitations](#limitations)
  - [Troubleshooting](#troubleshooting)

# ArgoCD GitOps Repository - Shop Application

This folder contains Kubernetes manifests and a Java Spring Boot backend for a shop application. PostgreSQL stores the product catalogue, while Redis stores shopping carts.

## Folder Structure

```text
ArgoCD/
├── app/                         # Java Spring Boot backend and Dockerfile
├── base/
│   ├── java-app.yaml            # Backend Deployment and Service
│   ├── postgres.yaml            # SQL product store
│   ├── redis.yaml               # NoSQL cart store
│   └── kustomization.yaml       # Base resources
├── environments/
│   ├── dev/kustomization.yaml   # shop-dev, two replicas, GHCR image
│   └── prod/kustomization.yaml  # shop-prod, three replicas
├── root-application.yaml        # ArgoCD Application
└── README.md
```

## System Architecture

```mermaid
flowchart LR
    git["GitHub repository"] --> app["ArgoCD Application<br/>shop-stack-dev"]
    app --> render["Kustomize overlay<br/>environments/dev"]
    render --> controller["ArgoCD controller"]
    controller --> backend["Java shop-backend"]
    controller --> postgres["PostgreSQL<br/>product catalogue"]
    controller --> redis["Redis<br/>shopping carts"]
    backend --> postgres
    backend --> redis
```

This diagram maps the `app`, `base`, `environments`, and `root-application.yaml` files in this folder to the ArgoCD-managed resources.

## What Is in the Manifests

- `root-application.yaml` points ArgoCD to the DEV overlay and enables automated sync, pruning, and self-healing.
- `base/java-app.yaml` configures the Java image, PostgreSQL and Redis service names, credentials, health probes, and Service.
- `base/postgres.yaml` creates `shopdb`, `shopuser`, and the PostgreSQL Service.
- `base/redis.yaml` creates the Redis Deployment and Service.
- `environments/dev/kustomization.yaml` uses the public GHCR image and two backend replicas.
- `environments/prod/kustomization.yaml` defines the production overlay with three replicas.
- `app/` contains the Spring Boot products API backed by PostgreSQL and carts API backed by Redis.

# Labs

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

## Section: IAM

### 9: Create developer user and RBAC permissions

In enterprise cloud environments (AWS EKS or Azure AKS), ArgoCD integrates directly with IAM (via AWS IAM Identity Center) or Entra ID (Azure AD) using OpenID Connect (OIDC). On Docker Desktop, we simulate this workflow using ArgoCD's built-in RBAC engine with local users or mock Dex OIDC tokens.

1. Enable local user management in ArgoCD by editing the `argocd-cm` ConfigMap:

```bash
kubectl patch configmap argocd-cm -n argocd --type merge -p '{"data":{"accounts.developer":"apiKey, login", "accounts.developer.enabled":"true"}}'
```

2. Assign a read-only RBAC policy for the developer user in the argocd-rbac-cm ConfigMap:

```bash
kubectl patch configmap argocd-rbac-cm -n argocd --type merge -p '{"data":{"policy.csv":"p, role:dev-readonly, applications, get, shop-dev/*, allow\np, role:dev-readonly, applications, sync, shop-dev/*, deny\ng, developer, role:dev-readonly"}}'
```

Set a password for the newly created developer user:

```bash
HASH=$(docker run --rm caddy caddy hash-password --plaintext 'DeveloperPass123')
kubectl patch secret argocd-secret -n argocd --type merge -p "{\"stringData\": {\"accounts.developer.password\": \"$HASH\", \"accounts.developer.mtime\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}}"
```

Optionally restart might be needed:

```bash
kubectl rollout restart deployment argocd-server -n argocd
kubectl rollout status deployment argocd-server -n argocd
```

Then re-enable ArgoCD UI after restart:

```bash
kubectl port-forward svc/argocd-server -n argocd 8088:443
```

3. Log in to ArgoCD UI:

- Open http://localhost:8088 in your browser.
- Log out from the admin account.
- Log in with credentials:
- Username: developer
- Password: DeveloperPass123

Expected result: Authentication succeeds, and the top right panel shows you are logged in as developer.

NOTE: As developer you cannot see any applications. Update RBAC role to be able to see it:

```bash
kubectl patch configmap argocd-rbac-cm -n argocd --type merge -p '{"data":{"policy.csv":"p, role:dev-readonly, applications, get, */*, allow\np, role:dev-readonly, applications, sync, */*, deny\np, role:dev-readonly, projects, get, *, allow\ng, developer, role:dev-readonly"}}'
```

### 10: Test Unauthorized Actions & Policy Enforcement

Verify that RBAC policies actively block restricted operations (such as manual triggers or synchronization) in the ArgoCD Web UI for non-admin roles.

View the application status in the Web UI (Allowed action):

- Click on the shop-stack-dev application tile.
- Expected result: You can successfully view application topology, resources, tree view, and live status (Synced / Healthy).

Attempt to trigger a manual sync via the UI (Forbidden action):

- Click the SYNC button at the top menu.
- Click SYNCHRONIZE.
- Expected result: The UI blocks the action and displays an error notification banner:
- PermissionDenied: permission denied: applications, sync, shop-dev/shop-stack-dev, deny

Note on Enterprise IAM Mapping (AWS / Azure):

- AWS IAM: OIDC claims map AWS SSO / IAM Identity Center groups (arn:aws:iam::123456789012:role/DeveloperRole) directly to role:dev-readonly in policy.csv.
- Azure Entra ID: Azure App Registration security group Object IDs (g, 90f0d111-2222-3333-4444-555555555555, role:dev-readonly) are matched in argocd-rbac-cm.

### 11: Inspect Audit Logs and Security Events

Audit and track changes, authorization failures, and administrative actions in ArgoCD.
Stream ArgoCD API Server logs to inspect denied access requests:

```bash
kubectl logs -n argocd -l app.kubernetes.io/name=argocd-server --tail=50 | grep -i "permission denied"
```

Expected result: Locate the audit entry generated by Step 16, detailing the timestamp, user (developer), requested action (sync), and denial reason.

Inspect Kubernetes Audit / Event log entries for unauthorized cluster-level activity in the target namespace:

```bash
kubectl get events -n shop-dev --sort-by='.lastTimestamp'
```

Verify application history and revision audits directly via ArgoCD:

```bash
kubectl get application shop-stack-dev -n argocd -o jsonpath='{range .status.history[*]}{"Revision: "}{.revision}{" | DeployedAt: "}{.deployedAt}{"\n"}{end}'
```

Expected result: Displays a complete audit trail of past synchronizations, including the commit SHA, timestamp, and author for every deployment in the GitOps pipeline.

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

## Section: CI/CD Pipeline & Automated Promotion (GitLab CI)

### 18: In GitLab CI build Java app and deploy it to Azure Container Apps

1. In Azure CloudShell create Azure SPN with needed permissions:

```bash
# 1. Create app
APP_ID=$(az ad app create --display-name "gitlab-ci-aca-<your_name>" --query appId -o tsv)

# 2. Create Service Principal for the app
az ad sp create --id $APP_ID

# 3. Get Subscription ID and Tenant ID
SUBSCRIPTION_ID=$(az account show --query id -o tsv)
TENANT_ID=$(az account show --query tenantId -o tsv)

# 4. Assign permissions (Contributor on subscription level)
az role assignment create \
  --role "Contributor" \
  --assignee $APP_ID \
  --scope "/subscriptions/$SUBSCRIPTION_ID"
```

and then configure Federated Credential for GitLab:

```bash
GITLAB_PROJECT_PATH="user-name-or-group/your-repo"

az ad app federated-credential create --id $APP_ID --parameters "{
    \"name\": \"gitlab-ci-main\",
    \"issuer\": \"https://gitlab.com\",
    \"subject\": \"project_path:${GITLAB_PROJECT_PATH}:ref_type:branch:ref:main\",
    \"description\": \"GitLab CI main branch authorization\",
    \"audiences\": [\"https://gitlab.com\"]
}"
```

Echo in CloudShell the variables to get the values. In case of problems use:

```bash
az ad app list --display-name "gitlab-ci-aca" --query "[0].appId" -o tsv
```

2. Set up required CI/CD variables in GitLab (Settings -> CI/CD -> Variables):
- AZURE_CLIENT_ID (value of APP_ID)
- AZURE_SUBSCRIPTION_ID
- AZURE_TENANT_ID
Mark variables as Masked and Expanded.

Create the .gitlab-ci.yml pipeline configuration targeting the test environment:

```yaml
stages:
  - build
  - deploy

variables:
  IMAGE_TAG: $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
  IMAGE_LATEST: $CI_REGISTRY_IMAGE:latest

  # Azure Configuration
  AZURE_RESOURCE_GROUP: "gitops-argocd-gitlab"
  AZURE_LOCATION: "westeurope"
  AZURE_CONTAINER_APP_ENV: "aca-env-gitlab"
  AZURE_APP_NAME: "aca-java-helloworld-gitlab"

# --- STAGE 1: Build and Push to GitLab Container Registry (GLCR) ---
build_and_push:
  stage: build
  image: docker:24.0.5
  services:
    - docker:24.0.5-dind
  script:
    - echo "Logging in to GitLab Container Registry..."
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY

    - echo "Building image..."
    - docker build -t $IMAGE_TAG -t $IMAGE_LATEST .

    - echo "Pushing image to GLCR..."
    - docker push $IMAGE_TAG
    - docker push $IMAGE_LATEST

# --- STAGE 2: Deploy to Azure Container Apps ---
deploy_to_aca:
  stage: deploy
  environment:
    name: azure
  image: mcr.microsoft.com/azure-cli:latest
  id_tokens:
    GITLAB_OIDC_TOKEN:
      aud: https://gitlab.com
  before_script:
    - echo "Logging in to Azure using OIDC Workload Identity Federation..."
    - az login --service-principal -u "$AZURE_CLIENT_ID" -t "$AZURE_TENANT_ID" --federated-token "$GITLAB_OIDC_TOKEN"
    - az account set --subscription "$AZURE_SUBSCRIPTION_ID"
  script:
    - echo "1. Creating Resource Group (if it does not exist)..."
    - az group create --name "$AZURE_RESOURCE_GROUP" --location "$AZURE_LOCATION"

    - echo "2. Creating Azure Container Apps Environment (free grant tier, if it does not exist)..."
    - >
      az containerapp env create \
        --name "$AZURE_CONTAINER_APP_ENV" \
        --resource-group "$AZURE_RESOURCE_GROUP" \
        --location $AZURE_LOCATION \
        --logs-destination none || true

    - echo "3. Deploying Java App to Azure Container Apps..."
    - >
      az containerapp create \
        --name "$AZURE_APP_NAME" \
        --resource-group "$AZURE_RESOURCE_GROUP" \
        --environment "$AZURE_CONTAINER_APP_ENV" \
        --image "$IMAGE_TAG" \
        --target-port 8080 \
        --ingress external \
        --min-replicas 0 \
        --max-replicas 1 \
        --registry-server $CI_REGISTRY \
        --registry-username $CI_REGISTRY_USER \
        --registry-password $CI_REGISTRY_PASSWORD
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
```

### 19: Connect Azure Container Apps to GitOps Repository via Built-in Flux CD
#### 19.1 Architectural Overview (Managed Flux in ACA)
Instead of maintaining a dedicated Kubernetes cluster running Argo CD, Azure Container Apps provides a native, managed Flux CD extension.

```bash
[GitLab CI] ─────────(Push Image)─────────> [GitLab Container Registry (GLCR)]
     │                                                     ▲
(Update Manifest)                                          │ (Pull Image)
     │                                                     │
     ▼                                                     │
[GitOps Repo] <──────(Polls Manifests)────── [Managed Flux CD in ACA] ──> [Container App]
```

Zero Infrastructure Overhead: Azure manages the Flux controllers internally; no control-plane nodes or pods to operate.
100% Free Tier Compliant: The GitOps extension operates within the ACA Consumption Plan without incurring extra control-plane charges.
Scale-to-Zero Intact: Flux updates the configuration dynamically without affecting your --min-replicas 0 idle state.

#### 19.2 GitOps Manifest Structure (manifests/containerapp.yaml)

1. In GitLab create Personal Access Token (PAT):
- Right top corner click on profile icon → Edit Profile (or Preferences) → Access Tokens → Add new token → Legacy type. 
- Token name: aca-gitops-pat
- Select scopes: Zaznacz write_repository and read_repository.

2. Inside your GitOps repository, place the native Azure Container App YAML manifest `apps/aca-java-helloworld/containerapp.yaml`. Flux will continuously monitor this file and reconcile changes.

```yaml
apiVersion: Microsoft.App/containerApps@2023-05-01
location: westeurope
name: aca-java-helloworld
properties:
  environmentId: /subscriptions/<SUBSCRIPTION_ID>/resourceGroups/rg-java-exercise/providers/Microsoft.App/managedEnvironments/env-java-exercise
  configuration:
    ingress:
      external: true
      targetPort: 8080
    registries:
      - server: registry.gitlab.com
        username: <GLCR_USERNAME>
        passwordSecretRef: glcr-password
    secrets:
      - name: glcr-password
        value: <GLCR_DEPLOY_TOKEN_OR_PAT>
  template:
    containers:
      - name: java-app
        image: registry.gitlab.com/your-org/your-repo/demo:latest
        resources:
          cpu: 0.25
          memory: 0.5Gi
    scale:
      minReplicas: 0
      maxReplicas: 1
```

then exchange `.gitla-ci.yml` file:

```yaml
stages:
  - build
  - deploy
  - post-deploy-tests

variables:
  IMAGE_TAG: $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
  IMAGE_LATEST: $CI_REGISTRY_IMAGE:latest
  CONTAINERAPP_NAME: "aca-java-helloworld-gitlab"
  RESOURCE_GROUP: "gitops-argocd-gitlab"

# --- STAGE 1: Build & Push Image ---
build_and_push:
  stage: build
  image: docker:24.0.5
  services:
    - docker:24.0.5-dind
  script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
    - docker build -t $IMAGE_TAG -t $IMAGE_LATEST .
    - docker push $IMAGE_TAG
    - docker push $IMAGE_LATEST
  rules:
    - if: $CI_COMMIT_BRANCH == "main"

# --- STAGE 2: Render Manifest & Reconcile Flux GitOps in Azure ---
deploy_gitops_flux:
  stage: deploy
  environment:
    name: azure
  image: mcr.microsoft.com/azure-cli:latest
  id_tokens:
    GITLAB_OIDC_TOKEN:
      aud: https://gitlab.com
  before_script:
    # Install gettext (envsubst) using Azure Linux package manager
    - tdnf install -y gettext git
    # 1. Azure OIDC Login
    - az login --service-principal -u "$AZURE_CLIENT_ID" -t "$AZURE_TENANT_ID" --federated-token "$GITLAB_OIDC_TOKEN"
    - az account set --subscription "$AZURE_SUBSCRIPTION_ID"
    - az extension add --name containerapp --upgrade
  script:
    # 2. Substitute variables in containerapp.yaml
    - echo "Substituting environment variables in containerapp.yaml..."
    - envsubst '${AZURE_SUBSCRIPTION_ID} ${GLCR_ACA_USERNAME} ${GLCR_ACA_PULL_TOKEN}' < apps/aca-java-helloworld/containerapp.yaml > apps/aca-java-helloworld/containerapp_rendered.yaml
    - mv apps/aca-java-helloworld/containerapp_rendered.yaml apps/aca-java-helloworld/containerapp.yaml

    # Update image tag to current CI commit SHA
    - |
      sed -i "s|image: .*|image: ${IMAGE_TAG}|g" apps/aca-java-helloworld/containerapp.yaml

    # 3. Commit and push rendered manifest to GitOps repository
    - git config user.email "gitlab-ci-bot@example.com"
    - git config user.name "GitLab CI Bot"
    - git add apps/aca-java-helloworld/containerapp.yaml
    - |
      if ! git diff-index --quiet HEAD; then
        git commit -m "chore(gitops): render secrets & set image tag to ${CI_COMMIT_SHA} [skip ci]"
        git push https://oauth2:${GITOPS_ACCESS_TOKEN}@gitlab.com/${CI_PROJECT_PATH}.git HEAD:main
      else
        echo "No changes detected in manifest."
      fi

    # 4. Create or update Native Managed Flux CD extension in ACA
    - echo "Deploying updated ACA manifest declaratively..."
    - |
      az containerapp update \
        --name "$CONTAINERAPP_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --yaml "apps/aca-java-helloworld/containerapp.yaml"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"

# --- STAGE 3: Post-Deploy Verification ---
verify_deployment:
  stage: post-deploy-tests
  environment:
    name: azure
  image: mcr.microsoft.com/azure-cli:latest
  id_tokens:
    GITLAB_OIDC_TOKEN:
      aud: https://gitlab.com
  before_script:
    - az login --service-principal -u "$AZURE_CLIENT_ID" -t "$AZURE_TENANT_ID" --federated-token "$GITLAB_OIDC_TOKEN"
    - az account set --subscription "$AZURE_SUBSCRIPTION_ID"
    - az extension add --name containerapp --upgrade
  script:
    - echo "Fetching Container App Provisioning State..."
    - |
      PROVISIONING_STATE=$(az containerapp show \
        --name "$CONTAINERAPP_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --query "properties.provisioningState" -o tsv)
      echo "Provisioning State: $PROVISIONING_STATE"

    - echo "Fetching Container App FQDN..."
    - APP_URL=$(az containerapp show --name "$CONTAINERAPP_NAME" --resource-group "$RESOURCE_GROUP" --query "properties.configuration.ingress.fqdn" -o tsv)
    - echo "Application URL https://${APP_URL}"

    - echo "Testing Application Endpoint (with retries for cold start)..."
    - |
      MAX_RETRIES=12
      RETRY_COUNT=0
      HTTP_STATUS="000"

      while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "https://${APP_URL}/" || echo "000")
        echo "Attempt $(($RETRY_COUNT + 1))/$MAX_RETRIES - HTTP Status Code: $HTTP_STATUS"
        
        if [ "$HTTP_STATUS" -eq 200 ]; then
          echo "Post-deploy verification PASSED!"
          exit 0
        fi
        
        RETRY_COUNT=$(($RETRY_COUNT + 1))
        echo "Waiting 5 seconds for Spring Boot to warm up..."
        sleep 5
      done

      echo "Post-deploy verification FAILED (HTTP Status: $HTTP_STATUS)!"
      exit 1
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
```

#### 19.3 End-to-End GitOps Deployment Workflow
Developer commits code to the Spring Boot Java repository.
GitLab CI pipeline builds the container image, tags it with $CI_COMMIT_SHA, and pushes it to GLCR.
GitLab CI promotion stage performs a git commit to update the image tag in manifests/containerapp.yaml within the GitOps repository.
Managed Flux CD in ACA detects the repository change (within the 1-minute sync interval) and applies the updated container revision automatically.

# Misc

## Use Cases

- Git-driven Kubernetes delivery with automated reconciliation.
- Self-healing after manual drift or pod deletion.
- A Java shop service using SQL for products and NoSQL for carts.
- Environment-specific scaling and image configuration with Kustomize.

## Limitations

Credentials are training values in manifests. PostgreSQL and Redis use Deployments without persistent volumes. The GHCR image must be public or referenced with an image pull Secret. `targetRevision: HEAD` follows the branch head, and automated pruning can delete resources removed from Git.

## Troubleshooting

```bash
kubectl get application shop-stack-dev -n argocd -o yaml
kubectl get events -n shop-dev --sort-by=.lastTimestamp
kubectl logs deployment/shop-backend -n shop-dev
kubectl rollout status deployment/shop-backend -n shop-dev
kubectl rollout restart deployment shop-backend -n shop-dev
```

For `ImagePullBackOff`, verify the GHCR package visibility and image tag. For `Progressing`, inspect probe failures and application logs. For `OutOfSync`, compare the ArgoCD revision with the GitHub branch.

See the detailed guide in [`docs/06-argocd.md`](../docs/06-argocd.md).
