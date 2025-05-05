gcloud container clusters get-credentials gke-demo --region us-central1 --project nth-autumn-354009
vim gke-container.yml
kubectl create namespace demo
kubectl apply -f gke-container.yml
