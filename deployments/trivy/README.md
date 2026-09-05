# 🛡️ Trivy Security Scanning (DevSecOps)

This directory integrates **Trivy**, a comprehensive and versatile security scanner, into our deployment pipeline. Integrating security checks into the development and deployment workflow converts our DevOps pipeline into a secure **DevSecOps** pipeline.

Trivy scans for:
1. **Container Image Vulnerabilities**: Finds known CVEs in built container images.
2. **Infrastructure as Code (IaC) Misconfigurations**: Scans Kubernetes manifests, Helm charts, and Docker Compose files for security risks (e.g., running as root, missing privilege escalation controls).
3. **Hardcoded Secrets**: Identifies exposed passwords, private keys, and API tokens.

---

## 🚀 Local Scanning

We provide a helper script, [scan.sh](file:///home/hoover/Projects/java/quarkus-grpc-payment_quarkus/deployments/trivy/scan.sh), to run Trivy scans locally via Docker. This eliminates the need to install Trivy directly on your machine.

### 📝 How to run

From the root directory of the project, run:
```bash
./deployments/trivy/scan.sh
```

The script will automatically perform:
* **Filesystem Scan**: Audits project dependencies, files, and searches for hardcoded secrets.
* **IaC Configuration Scan**: Scans all Kubernetes resource manifests under `deployments/kubernetes/base` and `deployments/local/docker-compose.yml` for security misconfigurations.

---

## ☸️ Kubernetes Operator (Continuous Scanning)

For production or dev/staging environments running inside Kubernetes (such as Minikube), you can deploy the **Trivy Operator**. 

The Trivy Operator runs as a controller inside the cluster, continuously scanning all running workloads (pods, deployments) and creating native Kubernetes custom resources with the audit results.

### 📦 1. Installation via Helm (Recommended)

To install Trivy Operator in your cluster, run:

```bash
# Add Aqua Security Helm repository
helm repo add aquasecurity https://aquasecurity.github.io/helm-charts/
helm repo update

# Install Trivy Operator in the 'trivy-system' namespace
helm install trivy-operator aquasecurity/trivy-operator \
  --namespace trivy-system \
  --create-namespace \
  --version 0.22.0
```

### 📊 2. Inspecting Security Reports

Trivy Operator automatically generates reports for your workloads inside the cluster. You can query these reports directly via `kubectl`:

#### View Workload Vulnerability Reports
Shows vulnerabilities found inside the container images of running pods:
```bash
kubectl get vulnerabilityreports -A
```

#### View Workload Misconfiguration Reports
Shows Kubernetes manifest security risks (e.g., privilege escalation enabled, missing CPU/Memory limits):
```bash
kubectl get configauditreports -A
```

#### View Exposed Secrets Reports
Shows if any sensitive environment variables or files are exposed inside the container runtime:
```bash
kubectl get exposedsecretreports -A
```

---

## 🛠️ DevSecOps Best Practices for This Project

1. **Pre-commit Checks**: Run `./deployments/trivy/scan.sh` locally before committing manifest changes to prevent pushing insecure configurations or credentials.
2. **CI Pipeline Integration**: Add Trivy scanning to your GitHub Actions / GitLab CI pipeline to fail the build if high or critical vulnerabilities/misconfigurations are found:
   ```yaml
   - name: Run Trivy vulnerability scanner
     uses: aquasecurity/trivy-action@master
     with:
       scan-type: 'fs'
       severity: 'CRITICAL,HIGH'
       exit-code: '1' # Fail build on findings
   ```
