[Source](https://artifacthub.io/packages/helm/elastic/elasticsearch)

# Install

- `helm repo add elastic https://helm.elastic.co`
- `helm repo update`
- `helm install elk-k8s --version <version> elastic/elasticsearch`

## Config

- `kubectl get pods --namespace=default -l app=elasticsearch-master -w2`
- `helm --namespace=default test elk-k8s`

## Check deployment

- `helm ls`

# Uninstall

- `helm delete elk-k8s`
