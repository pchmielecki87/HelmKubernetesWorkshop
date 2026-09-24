# Labs

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