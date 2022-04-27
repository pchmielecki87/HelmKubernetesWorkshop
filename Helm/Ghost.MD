[Source](https://artifacthub.io/packages/helm/bitnami/ghost)


helm repo add bitnami https://charts.bitnami.com/bitnami
helm install ghost-k8s bitnami/ghost


NOTE: It may take a few minutes for the LoadBalancer IP to be available.
        Watch the status with: 'kubectl get svc --namespace default -w ghost-k8s'

  export APP_HOST=$(kubectl get svc --namespace default ghost-k8s --template "{{ range (index .status.loadBalancer.ingress 0) }}{{ . }}{{ end }}")

  export GHOST_PASSWORD=$(kubectl get secret --namespace "default" ghost-k8s -o jsonpath="{.data.ghost-password}" | base64 --decode)
  export MARIADB_ROOT_PASSWORD=$(kubectl get secret --namespace "default" ghost-k8s-mariadb -o jsonpath="{.data.mariadb-root-password}" | base64 --decode)
  export MARIADB_PASSWORD=$(kubectl get secret --namespace "default" ghost-k8s-mariadb -o jsonpath="{.data.mariadb-password}" | base64 --decode)

2. Complete your Ghost deployment by running:

  helm upgrade --namespace default ghost-k8s bitnami/ghost \
    --set service.type=LoadBalancer,ghostHost=$APP_HOST,ghostPassword=$GHOST_PASSWORD,mariadb.auth.rootPassword=$MARIADB_ROOT_PASSWORD,mariadb.auth.password=$MARIADB_PASSWORD

