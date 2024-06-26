# Istio

## Install

`https://istio.io/latest/docs/setup/install/`

## Konfiguracja i wdroenie

istioctl install --set profile=demo -y
kubectl label namespace default istio-injection=enabled

kubectl apply -f bookinfo.yaml
kubectl apply -f gateway.yaml

## Testowanie

export INGRESS_HOST=$(kubectl get po -l app=istio-ingressgateway -n istio-system -o jsonpath='{.items[0].status.hostIP}')
export INGRESS_PORT=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')

http://$INGRESS_HOST:$INGRESS_PORT/productpage

### Jeśli nie działa

kubectl port-forward -n istio-system svc/istio-ingressgateway 8080:80
`http://localhost:8080/productpage`
