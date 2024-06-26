Go to: HelmKubernetesWorkshop/Kustomize/

kustomize build $PWD/overlays/dev | kubectl apply -f -
kustomize build $PWD/overlays/prod | kubectl apply -f -
