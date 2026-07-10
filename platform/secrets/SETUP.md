# Secrets: move database credentials into AWS Secrets Manager

In Phase 1 the database password lived in a Kubernetes Secret you created by hand. In Phase 2 it lives in AWS Secrets Manager, and the External Secrets Operator pulls it into the cluster automatically. No password is ever written into a file, a chart, or git.

## 1. Put the credentials in Secrets Manager

```bash
aws secretsmanager create-secret \
  --name shopflow/db \
  --secret-string '{"username":"shopflow","password":"YOUR_DB_PASSWORD"}' \
  --region us-east-1
```

## 2. Install the External Secrets Operator

```bash
helm repo add external-secrets https://charts.external-secrets.io
helm repo update
helm install external-secrets external-secrets/external-secrets \
  -n external-secrets --create-namespace
```

## 3. Give it read access via IRSA

Create an IAM policy that allows `secretsmanager:GetSecretValue` on `shopflow/db`, then bind it to a service account the operator uses:

```bash
eksctl create iamserviceaccount \
  --cluster shopflow --region us-east-1 \
  --namespace default --name external-secrets-sa \
  --attach-policy-arn arn:aws:iam::ACCOUNT:policy/ShopflowSecretsRead \
  --approve
```

(Create the `ShopflowSecretsRead` policy first with `aws iam create-policy`, allowing `secretsmanager:GetSecretValue` on the secret's ARN.)

## 4. Apply the External Secret

```bash
kubectl apply -f external-secret.yaml
kubectl get secret shopflow-db        # created automatically from Secrets Manager
```

Once `shopflow-db` exists, the services read their database credentials from it exactly as before, but the source of truth is now Secrets Manager.

## Why this is better

The password is stored, rotated, and audited in one managed place. Developers never see it, it is never in the repo, and the pods get it through a short-lived identity (IRSA) rather than a static key.
