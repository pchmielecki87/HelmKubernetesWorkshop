[Source](https://artifacthub.io/packages/helm/bitnami/nginx)

# Install
`helm repo add bitnami https://charts.bitnami.com/bitnami`
`helm install simplenginx bitnami/nginx`

## Config

`export SERVICE_PORT=$(kubectl get --namespace default -o jsonpath="{.spec.ports[0].port}" services simplenginx)`
`export SERVICE_IP=$(kubectl get svc --namespace default simplenginx -o jsonpath='{.status.loadBalancer.ingress[0].ip}')`
`echo "http://${SERVICE_IP}:${SERVICE_PORT}"`

## Check

`helm ls`

# Uninstall
`helm delete simplenginx`