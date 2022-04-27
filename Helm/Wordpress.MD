[Source](https://artifacthub.io/packages/helm/bitnami/wordpress)


helm repo add bitnami https://charts.bitnami.com/bitnami
helm install wordpress-k8s bitnami/wordpress

our WordPress site can be accessed through the following DNS name from within your cluster:

    wordpress-k8s.default.svc.cluster.local (port 80)

To access your WordPress site from outside the cluster follow the steps below:

1. Get the WordPress URL by running these commands:

  NOTE: It may take a few minutes for the LoadBalancer IP to be available.
        Watch the status with: 'kubectl get svc --namespace default -w wordpress-k8s'

   export SERVICE_IP=$(kubectl get svc --namespace default wordpress-k8s --include "{{ range (index .status.loadBalancer.ingress 0) }}{{ . }}{{ end }}")
   echo "WordPress URL: http://$SERVICE_IP/"
   echo "WordPress Admin URL: http://$SERVICE_IP/admin"