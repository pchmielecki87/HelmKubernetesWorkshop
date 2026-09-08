# Istio Installation Notes

## Install

Follow the official installation guide: <https://istio.io/latest/docs/setup/install/>.

## Configure and Deploy

istioctl install --set profile=demo -y
kubectl label namespace default istio-injection=enabled

Apply the manifests from the parent folder:

```bash
kubectl apply -f ../backend.yaml
kubectl apply -f ../frontend.yaml
kubectl apply -f ../istio-gateway.yaml
kubectl apply -f ../istio-virtualservice.yaml
kubectl apply -f ../istio-destinationrule.yaml
```

## Test

export INGRESS_HOST=$(kubectl get po -l app=istio-ingressgateway -n istio-system -o jsonpath='{.items[0].status.hostIP}')
export INGRESS_PORT=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')

curl "http://$INGRESS_HOST:$INGRESS_PORT"

## Troubleshooting

```bash
kubectl port-forward -n istio-system svc/istio-ingressgateway 8080:80
curl http://localhost:8080
istioctl analyze
```
