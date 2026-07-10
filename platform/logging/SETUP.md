# Logging: ship container logs to CloudWatch

Instead of running `kubectl logs` pod by pod, send every container's logs to a single CloudWatch log group you can search.

## 1. Give the log shipper permission (IRSA)

```bash
eksctl create iamserviceaccount \
  --cluster shopflow --region us-east-1 \
  --namespace amazon-cloudwatch --name aws-for-fluent-bit \
  --attach-policy-arn arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy \
  --approve
```

## 2. Install Fluent Bit

```bash
helm repo add eks https://aws.github.io/eks-charts
helm repo update
helm install aws-for-fluent-bit eks/aws-for-fluent-bit \
  -n amazon-cloudwatch --create-namespace \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-for-fluent-bit \
  -f fluent-bit-values.yaml
```

## 3. View the logs

In the AWS console, open CloudWatch, Log groups, `/shopflow/containers`. You will see streams for every pod. Place an order in the storefront, then find the confirmation line from the Notifications worker here instead of with `kubectl logs`.

## Why this matters

In production you cannot tail pods by hand, especially once autoscaling creates and destroys them. Central logging means the history survives the pod, and you can search across all services at once.
