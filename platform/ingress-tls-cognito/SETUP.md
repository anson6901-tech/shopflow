# HTTPS and login: TLS with ACM, authentication with Cognito

This puts the app behind HTTPS and forces users to log in before they can reach it. The load balancer does both, so the application code stays simple.

You need a domain name you control (in Route 53 or pointed at AWS).

## 1. Get a TLS certificate from ACM

```bash
aws acm request-certificate \
  --domain-name shop.YOURDOMAIN.com \
  --validation-method DNS \
  --region us-east-1
```

Follow the console to add the DNS validation record. When the certificate is Issued, copy its ARN into `helm/shopflow/values.yaml` as `ingress.certificateArn`.

## 2. Create a Cognito user pool

```bash
aws cognito-idp create-user-pool --pool-name shopflow-users --region us-east-1
```

Then, in the Cognito console: add an app client, set the callback URL to `https://shop.YOURDOMAIN.com/oauth2/idpresponse`, and set a hosted-UI domain. Copy the user pool ARN, the app client id, and the domain into `values.yaml` under `ingress.cognito`.

## 3. Point your domain at the load balancer

After Helm creates the Ingress, get the load balancer hostname:

```bash
kubectl get ingress shopflow
```

Create a Route 53 record (an alias or CNAME) for `shop.YOURDOMAIN.com` pointing at that hostname.

## 4. Test

Open `https://shop.YOURDOMAIN.com`. You should be redirected to a Cognito login page first. After you sign in, the store loads, over HTTPS.

## What this teaches

Authentication and encryption are handled at the edge by the load balancer, configured entirely through Ingress annotations. The services behind it never see an unauthenticated request, and never handle a raw certificate. This is a common, clean enterprise pattern.
