# ShopFlow, Phase 1: Build and Run the System

This is Phase 1 of the capstone. By the end you will have a small but real microservices system running on AWS: a web storefront, three backend services that talk to each other, a managed database, and a message queue, all on Kubernetes. Phase 2 then turns this into a fully operated platform (monitoring, security hardening, CI/CD, autoscaling).

Read `PROJECT-6-CAPSTONE-DESIGN` first if you have not. It explains, in plain language, what every piece is. This README is the hands-on build.

## What you are building (quick recap)

Four small programs that work together:

- **Storefront** (React): the web page customers use. It also forwards API calls to the backends.
- **Catalog service** (Spring Boot): knows about products, reads them from the database.
- **Orders service** (Spring Boot): places orders. It calls Catalog to check the product, saves the order, then drops an event on the queue.
- **Notifications worker** (Spring Boot): watches the queue and "sends" a confirmation for each order.

The flow of one order: browser, to Storefront, to Orders, which calls Catalog and writes to the database, then publishes to SQS, which the Notifications worker consumes. That single path is the whole lesson: frontend to backend, backend to backend (synchronous), backend to database, and backend to backend (asynchronous through a queue).

See `architecture.svg` in this folder for a diagram of exactly what Phase 1 deploys, and what Phase 2 will add on top.

## What is in this folder

```
infra/        Terraform that creates the AWS environment (VPC, EKS, RDS, ECR, SQS)
services/     the three Spring Boot backends (catalog, orders, notifications)
storefront/   the React web app
k8s/          the Kubernetes manifests that deploy everything
```

## What Phase 1 includes, and what waits for Phase 2

Phase 1 gets the system built and running, with sensible baseline security: the database is in private subnets, credentials come from a Kubernetes Secret, and services reach SQS through the node's IAM role.

Deliberately deferred to Phase 2, so this phase stays learnable: Cognito login, HTTPS with a real certificate, secrets in AWS Secrets Manager via IRSA, Prometheus and Grafana monitoring, autoscaling, Helm packaging, and wiring each service into the CI/CD pipeline. You will add those on top of the working system.

## Prerequisites

- An AWS account and the AWS CLI configured (`aws configure`) with admin-level credentials.
- `terraform`, `kubectl`, `eksctl`, `helm`, and `docker` installed.
- Install help for kubectl, eksctl, and the AWS CLI is in the Project 4 and 5 guides.

A real cost warning: this creates an EKS cluster, two worker nodes, a NAT gateway, an RDS database, and a load balancer. That is several dollars a day. Build it, learn from it, and tear it down the same day (see Cleanup).

## Step 1: Create the AWS environment with Terraform

Terraform reads the files in `infra/` and creates everything for you.

```bash
cd infra
export TF_VAR_db_password='ChooseAStrongPassword123!'   # do not commit this
terraform init
terraform apply        # review the plan, type yes
```

This takes roughly 15 to 20 minutes (EKS and RDS are slow to create). When it finishes, look at the outputs:

```bash
terraform output
```

You will use `cluster_name`, `ecr_repository_urls`, `rds_endpoint`, and `order_events_queue_name`. Keep this terminal open.

## Step 2: Connect kubectl to the cluster

```bash
aws eks update-kubeconfig --region us-east-1 --name shopflow
kubectl get nodes        # you should see two nodes Ready
```

## Step 3: Install the AWS Load Balancer Controller (one time)

The Ingress needs this controller to create the load balancer. This is the fiddliest step; run it once.

```bash
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)

eksctl utils associate-iam-oidc-provider --cluster shopflow --region us-east-1 --approve

curl -fsSL -o iam-policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.7.2/docs/install/iam_policy.json
aws iam create-policy --policy-name AWSLoadBalancerControllerIAMPolicy --policy-document file://iam-policy.json || true

eksctl create iamserviceaccount \
  --cluster shopflow --region us-east-1 \
  --namespace kube-system --name aws-load-balancer-controller \
  --attach-policy-arn arn:aws:iam::$ACCOUNT:policy/AWSLoadBalancerControllerIAMPolicy \
  --approve --role-name AmazonEKSLoadBalancerControllerRole

helm repo add eks https://aws.github.io/eks-charts
helm repo update
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=shopflow \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller

kubectl get deployment -n kube-system aws-load-balancer-controller   # wait for it to be Available
```

## Step 4: Build and push the four images to ECR

```bash
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
REGION=us-east-1
REGISTRY=$ACCOUNT.dkr.ecr.$REGION.amazonaws.com

aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $REGISTRY

# the three backends
for svc in catalog orders notifications; do
  docker build -t $REGISTRY/shopflow-$svc:1.0 services/$svc-service
  docker push $REGISTRY/shopflow-$svc:1.0
done

# the storefront
docker build -t $REGISTRY/shopflow-storefront:1.0 storefront
docker push $REGISTRY/shopflow-storefront:1.0
```

## Step 5: Fill in the configuration

Point the config at your database, create the credentials Secret, and put your image names into the manifests.

```bash
# 1. database host into the ConfigMap
RDS=$(cd infra && terraform output -raw rds_endpoint | cut -d: -f1)
sed -i "s|REPLACE_RDS_ENDPOINT|$RDS|" k8s/shared-config.yaml

# 2. the database password Secret
cp k8s/db-secret.example.yaml k8s/db-secret.yaml
sed -i "s|REPLACE_WITH_DB_PASSWORD|$TF_VAR_db_password|" k8s/db-secret.yaml

# 3. image names into the deployments
sed -i "s|REPLACE_CATALOG_IMAGE|$REGISTRY/shopflow-catalog:1.0|"             k8s/catalog.yaml
sed -i "s|REPLACE_ORDERS_IMAGE|$REGISTRY/shopflow-orders:1.0|"               k8s/orders.yaml
sed -i "s|REPLACE_NOTIFICATIONS_IMAGE|$REGISTRY/shopflow-notifications:1.0|" k8s/notifications.yaml
sed -i "s|REPLACE_STOREFRONT_IMAGE|$REGISTRY/shopflow-storefront:1.0|"       k8s/storefront.yaml
```

If your region is not `us-east-1`, also change `AWS_REGION` in `k8s/shared-config.yaml`.

## Step 6: Deploy

```bash
kubectl apply -f k8s/shared-config.yaml
kubectl apply -f k8s/db-secret.yaml
kubectl apply -f k8s/catalog.yaml
kubectl apply -f k8s/orders.yaml
kubectl apply -f k8s/notifications.yaml
kubectl apply -f k8s/storefront.yaml
kubectl apply -f k8s/ingress.yaml

kubectl get pods        # wait until all are Running and Ready
```

## Step 7: Open the app and place an order

Get the load balancer address the Ingress created (it takes two or three minutes to appear):

```bash
kubectl get ingress shopflow
```

Open the `ADDRESS` shown (an `...elb.amazonaws.com` hostname) in your browser. You should see the store with four products. Click Buy on one. You should see an order confirmation with an order id.

Now prove the asynchronous half worked, by reading the Notifications worker's logs:

```bash
kubectl logs -l app=notifications-service --tail=20
```

You should see a line like `Confirmation sent for order event: {...}`. That message traveled from Orders, through SQS, to Notifications, with the two services never talking directly. That is the whole system working.

## Troubleshooting

- **Pods stuck in `ImagePullBackOff`.** The image name in the manifest is wrong, or the push failed. Recheck Step 4 and the `sed` in Step 5.
- **Catalog or Orders pods `CrashLoopBackOff`.** Usually the database. Confirm the RDS host in `k8s/shared-config.yaml`, the password in `k8s/db-secret.yaml`, and that RDS finished creating. `kubectl logs <pod>` shows the real error.
- **Buying a product returns an error.** Check the Orders logs (`kubectl logs -l app=orders-service`). If it cannot reach Catalog, confirm the Catalog pods are Ready and the service name is `catalog-service`.
- **No confirmation in the Notifications logs.** The worker cannot read SQS. Confirm `ORDER_EVENTS_QUEUE_NAME` and `AWS_REGION` in the ConfigMap match your queue, and that Terraform attached the SQS policy to the node role.
- **`kubectl get ingress` shows no address.** The load balancer controller is not healthy. Recheck Step 3 and `kubectl logs -n kube-system deploy/aws-load-balancer-controller`.

## Cleanup (do this when you finish, it costs money)

```bash
kubectl delete -f k8s/ingress.yaml      # delete the load balancer first
kubectl delete -f k8s/                  # then the rest
cd infra && terraform destroy           # then the whole environment
```

If `terraform destroy` complains that the VPC is in use, the load balancer from the Ingress was not deleted yet. Delete the Ingress, wait a minute, and run destroy again.

## A note from your instructor

This is a large, real codebase. It was written to be correct and to teach, but you are the one who runs it, and the first run of any distributed system on AWS rarely goes perfectly. Treat the errors as the lesson: read the logs, fix one thing, try again. That debugging is exactly the work you will describe to a hiring manager. Keep a short log of the problems you solved; those become your interview stories.


# ShopFlow, Phase 2: Operate It as a Platform

Phase 1 got the system running. Phase 2 turns it into something a real team would run in production: shipped automatically through a pipeline, monitored, secured, scalable, and packaged with Helm. You add these on top of the working Phase 1 system, one capability at a time.

`architecture.svg` in this folder shows the full production architecture, the end state you are building toward here.

## Prerequisite

Phase 1 is deployed and the order flow works. You have the EKS cluster, RDS, SQS, ECR, and the AWS Load Balancer Controller from Phase 1.

## What Phase 2 adds, and where each part lives

- **Helm packaging** (`helm/shopflow/`): the four services repackaged as one chart, now with hardened pods, resource limits, autoscaling, network policies, an IRSA service account, a Prometheus ServiceMonitor, and the secure ingress. This replaces the hand-applied manifests from Phase 1.
- **Security** (`platform/secrets/`): database credentials move to AWS Secrets Manager, pulled in by the External Secrets Operator. Pods reach SQS and secrets through IRSA, not static keys.
- **HTTPS and login** (`platform/ingress-tls-cognito/`): TLS with an ACM certificate and Cognito login enforced at the load balancer.
- **Monitoring** (`platform/monitoring/`): Prometheus, Grafana dashboards, and alerts.
- **Logging** (`platform/logging/`): all container logs shipped to CloudWatch.
- **CI/CD** (`cicd/`): each service shipped through the gated pipeline, publishing to Nexus and deploying with Helm.
- **Testing** (`testing/`): integration tests with Testcontainers, a contract-test path, and a post-deploy smoke test.

Each folder has its own SETUP.md with exact commands. Below is the order to do them in.

## Changes to carry over from Phase 1 first

Two small code changes make the Phase 2 hardening work:

1. **Metrics.** Add `io.micrometer:micrometer-registry-prometheus` (runtime scope) to each backend `pom.xml` so `/actuator/prometheus` serves metrics. See `platform/monitoring/SETUP.md`.
2. **Non-root storefront.** The hardened storefront runs as a non-root user, which cannot bind to port 80. In the Phase 1 storefront, change `nginx.conf` `listen 80;` to `listen 8080;`, and the Dockerfile base image from `nginx:1.27-alpine` to `nginxinc/nginx-unprivileged:1.27-alpine`. Rebuild and push.

## Recommended order

1. **Secrets** (`platform/secrets/SETUP.md`): create the Secrets Manager secret, install External Secrets, apply `external-secret.yaml`. Confirm the `shopflow-db` Secret appears.
2. **IRSA for the app**: create an IAM role that allows SQS access and bind it to the `shopflow` service account; put its ARN in `values.yaml` as `serviceAccount.roleArn`.
3. **Monitoring** (`platform/monitoring/SETUP.md`): install kube-prometheus-stack, apply the alert rules.
4. **Logging** (`platform/logging/SETUP.md`): install Fluent Bit.
5. **TLS and Cognito** (`platform/ingress-tls-cognito/SETUP.md`): request the certificate, create the Cognito pool, fill the `ingress` values.
6. **Deploy with Helm** (below).
7. **CI/CD** (`cicd/README.md`): wire the pipelines so future changes ship automatically.

Tip: until the certificate and Cognito values are ready, you can keep using the Phase 1 HTTP ingress and skip the chart's ingress, so you can test the rest sooner.

## Deploy with Helm

Fill in every `REPLACE_` value in `helm/shopflow/values.yaml` (registry, IRSA role ARN, RDS endpoint, certificate, Cognito). Then check the rendering before applying anything:

```bash
helm lint helm/shopflow
helm template shopflow helm/shopflow      # read the output, confirm it looks right
helm upgrade --install shopflow helm/shopflow
kubectl get pods
```

`helm template` first is the habit to build: it shows you exactly what will be applied, so you catch a wrong value before it hits the cluster.

## Verify Phase 2

- **Secrets:** `kubectl get secret shopflow-db` exists and the services start without a hand-made secret.
- **Monitoring:** the Grafana dashboard shows traffic after you place orders; an alert fires if you scale a service to zero.
- **Autoscaling:** `kubectl get hpa` shows targets; under load the replica count rises.
- **Security:** the site is reachable only over HTTPS and redirects to Cognito login; network policies block traffic that should not exist.
- **CI/CD:** a push builds, gates, publishes to Nexus, and deploys through Helm on its own.

## Cost and teardown

This runs everything Phase 1 did, plus the monitoring stack and more. The cost is real. To tear down: `helm uninstall shopflow`, uninstall the add-on Helm releases (monitoring, logging, external-secrets), delete the Ingress so the load balancer goes away, then `terraform destroy` in the Phase 1 `infra/` folder.

## A note on what could not be tested here

This package was written carefully, but the TLS, Cognito, Secrets Manager, IRSA, and monitoring pieces are specific to your AWS account and domain, and the Helm templates need your real values. Always run `helm lint` and `helm template` before installing, complete one SETUP.md at a time, and verify each before moving on. The errors you fix here are real platform-engineering work, and good interview material.

## The full story you can now tell

With both phases done, ShopFlow is a complete portfolio project: a microservices system on AWS EKS, with a React storefront and Spring Boot services communicating over REST and SQS, backed by RDS, fronted by an ALB with TLS and Cognito, secrets in Secrets Manager via IRSA, shipped through a gated Jenkins pipeline with Nexus and scanning, observed with Prometheus, Grafana, and central logging, autoscaled, network-segmented, and packaged with Helm and Terraform. That is a system you built, can draw, and can explain end 