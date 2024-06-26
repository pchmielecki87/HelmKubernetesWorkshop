# Helm

## Source

[Source](https://artifacthub.io/packages/helm/grafana/grafana)
**Works locally**

## Install

- `helm repo add grafana https://grafana.github.io/helm-charts`
- `helm repo update`
- `helm install grafana-k8s grafana/grafana`

## Config

1. Get your 'admin' user password by running: `kubectl get secret --namespace default grafana-k8s -o jsonpath="{.data.admin-password}" | base64 --decode ; echo`
2. The Grafana server can be accessed via port 80 on the following DNS name from within your cluster: `grafana-k8s.default.svc.cluster.local`
3. Get the Grafana URL to visit by running these commands in the same shell:
   1. `export POD_NAME=$(kubectl get pods --namespace default -l "app.kubernetes.io/name=grafana,app.kubernetes.io/instance=grafana-k8s" -o jsonpath="{.items[0].metadata.name}")`
   2. `kubectl --namespace default port-forward $POD_NAME 3000`
4. Login with the password from step 1 and the username: `admin` on `127.0.0.1:3000`

## Check deployment

- `helm ls`
- check application on `localhost:3000`
- username: `admin`

## Uninstall

- `helm delete grafana-k8s`
