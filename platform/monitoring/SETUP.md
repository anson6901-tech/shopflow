# Monitoring: Prometheus, Grafana, and alerts

This gives you live dashboards and alerts for every service.

## 1. Make the services expose Prometheus metrics

Add this dependency to each Spring Boot service's `pom.xml`, then rebuild and push the images:

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
  <scope>runtime</scope>
</dependency>
```

The services already expose `prometheus` in their Actuator config, so once this dependency is present, `/actuator/prometheus` serves metrics.

## 2. Install the monitoring stack

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  -n monitoring --create-namespace
```

This installs Prometheus, Grafana, Alertmanager, and the Operator CRDs that the `ServiceMonitor` and `PrometheusRule` use.

## 3. Apply the ShopFlow monitoring objects

The `ServiceMonitor` ships inside the Helm chart, so it is already applied. Add the alert rules:

```bash
kubectl apply -f prometheus-rules.yaml
```

## 4. See the dashboards

```bash
kubectl port-forward -n monitoring svc/kube-prometheus-stack-grafana 3000:80
```

Open `http://localhost:3000` (default login `admin` / `prom-operator`). Import `grafana-dashboard.json` (Dashboards, New, Import) to see request rate, latency, and error rate per service.

## What to look for

After placing a few orders in the storefront, the request-rate panel should show traffic on `catalog-service` and `orders-service`. That is your system observed in production, the same screen an on-call engineer would watch.
