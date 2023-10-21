kustomize build $PWD/HelmKubernetesWorkshop/kustomize/overlays/dev | kubectl apply -f -
kustomize build $PWD/HelmKubernetesWorkshop/kustomize/overlays/prod | kubectl apply -f -
