kustomize build kustomize/overlays/dev | kubectl apply -f -
kustomize build kustomize/overlays/prod | kubectl apply -f -
