# Helm Kubernetes Workshop

This repository is a practical workshop for Docker, Kubernetes, Helm, Kustomize, Kured, ArgoCD, and Istio. Examples are designed for local learning with Docker Desktop Kubernetes, with a separate GKE example under `Kubernetes/`.

## Documentation

Start with the English documentation index:

- [Workshop documentation index](docs/00-index.md)
- [Docker](Docker/README.md)
- [Kubernetes](Kubernetes/README.md)
- [Helm](Helm/README.md)
- [Kured](Kured/README.md)
- [Kustomize](Kustomize/README.md)
- [ArgoCD](ArgoCD/README.md)
- [Istio](Istio/README.md)

## Suggested Learning Path

1. Build and run containers with Docker.
2. Apply basic Kubernetes workloads.
3. Reuse manifests with Kustomize.
4. Install packaged services with Helm.
5. Explore Istio traffic management and Kured node operations.
6. Finish with GitOps deployment through ArgoCD.

## Local Prerequisites

- Docker Desktop with Kubernetes enabled.
- `kubectl`, `docker`, and `helm` on `PATH`.
- `istioctl` for the Istio examples.
- `jq` for the Kured release installation command.