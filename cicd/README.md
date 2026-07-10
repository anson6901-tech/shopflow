# CI/CD: ship each service through the gated pipeline

In Phase 1 you built and deployed by hand. In Phase 2 every push goes through the pipeline you built in Project 5, extended to publish artifacts to Nexus and deploy with Helm.

## What the pipeline does

For a service, on each push: build, test with coverage, SonarQube quality gate, Trivy dependency scan, package, publish the jar to Nexus, build the image, Trivy image scan, push to ECR, and deploy with Helm. If any gate fails, nothing ships.

## Setup

1. Stand up Jenkins, SonarQube, and Nexus (reuse your Project 5 stack; add a Nexus container, or use a hosted Nexus).
2. Add Jenkins credentials: `aws-ecr`, `sonar-token`, and `nexus` (username and password).
3. Create one pipeline job per service (`catalog`, `orders`, `notifications`), each pointing at this repo with `cicd/Jenkinsfile` as the script path, and set the `SERVICE` parameter.
4. The storefront uses a simpler npm-and-docker build; add a small separate job for it if you want it automated.

## The artifact store

The `Publish artifact to Nexus` stage uploads the built jar to Nexus. That gives you a versioned history of every build output, separate from the runtime images in ECR. `settings-nexus.xml` documents the server id the deploy step uses.

## The deploy

The `Deploy with Helm` stage runs `helm upgrade --install`, so the same chart that defines your platform is what ships each change. Because it uses `--reuse-values`, your environment settings stay put and only the image tag moves forward.
