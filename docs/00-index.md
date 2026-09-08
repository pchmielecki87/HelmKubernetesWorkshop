# Helm Kubernetes Workshop Documentation 📚

This documentation set describes the seven practical areas in this repository. Each guide explains the files that are actually present, the architecture they demonstrate, how to run the examples, realistic use cases, limitations, and troubleshooting steps.

## Contents 🗂️

1. [Docker](01-docker.md) - Images, Dockerfiles, Compose, volumes, and local containers.
2. [Kubernetes](02-kubernetes.md) - Pods and workload controllers such as Deployments, ReplicaSets, StatefulSets, and DaemonSets.
3. [Helm](03-helm.md) - Packaging, rendering, installing, and managing Kubernetes charts.
4. [Kured](04-kured.md) - Automated Kubernetes node reboots and the scheduled restart example.
5. [Kustomize](05-kustomize.md) - Base manifests and dev/prod overlays without templating.
6. [ArgoCD](06-argocd.md) - GitOps reconciliation for the Java shop application.
7. [Istio](07-istio.md) - Ingress routing, VirtualServices, and traffic policies.

## Recommended Path 🧭

Start with Docker, then Kubernetes fundamentals, Kustomize, Helm, and Istio. Kured and ArgoCD are operational topics that make more sense after the workload and deployment concepts are familiar.

## Documentation Conventions 📝

- Commands are written for macOS or Linux shells.
- Examples use Docker Desktop Kubernetes unless a guide explicitly mentions GKE.
- Values such as passwords, image tags, and public endpoints are training defaults and must be replaced for production.
- Render or validate manifests before applying them to a shared cluster.
