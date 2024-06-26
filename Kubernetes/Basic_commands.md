# Kubernetes

## List pods

kubectl get pods
kubectl get pods -A / kubectl get pods --all-namespaces
kubectl get pods --namespace=default

## Create simple deployment

kubectl create deployment internal-deployment --image=nginx --replicas=2

## Create resources

kubectl apply -f /path/to/file.yaml

## Expose service

kubectl expose deployment internal-deployment --port=80 --target-port=8008 --name=internal-service

## Describe

kubectl describe pod `pod_name`

## Get logs

kubectl logs `pod_name`

## Login to pod with /bin/bash

kubectl exec --tty --stdin `pod_name` -- /bin/bash

## Delete

kubectl delete pod `pod_name`

## Documentation

`https://kubernetes.io/docs/concepts/workloads/pods/`
`https://k21academy.com/docker-kubernetes/multi-container-pods/`
`https://kubernetes.io/docs/concepts/workloads/controllers/deployment/`
`https://kubernetes.io/docs/concepts/workloads/controllers/replicaset/`
`https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/`
`https://kubernetes.io/docs/concepts/workloads/controllers/daemonset/`
