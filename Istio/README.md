kubectl apply -f Istio/

Now, you should have the frontend and backend services deployed in your cluster, and Istio should be configured to manage the traffic between them. The frontend service should call the backend service when accessed through the Istio Ingress Gateway.