[Source](https://artifacthub.io/packages/helm/jenkinsci/jenkins)

**Works locally**

# Install
- `helm repo add jenkins https://charts.jenkins.io`
- `helm repo update`
- `helm install jenkins-k8s jenkins/jenkins [flags]`

## Config

1. Get your 'admin' user password by running: `kubectl exec --namespace default -it svc/jenkins-k8s -c jenkins -- /bin/cat /run/secrets/chart-admin-password && echo`
2. Get the Jenkins URL to visit by running these commands in the same shell:
  `echo http://127.0.0.1:8080`
  `kubectl --namespace default port-forward svc/jenkins-k8s 8080:8080`

3. Login with the password from step 1 and the username: `admin`
4. Configure security realm and authorization strategy
5. Use Jenkins Configuration as Code by specifying configScripts in your values.yaml file, see [documentation](http:///configuration-as-code) and [examples](https://github.com/jenkinsci/configuration-as-code-plugin/tree/master/demos)

## Check deployment

- `helm ls`

# Uninstall
-`helm delete jenkins-k8s`