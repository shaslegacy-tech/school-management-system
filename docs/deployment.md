# Production Deployment Strategy

## Common topology

Publish immutable web/API images from protected release tags. Promote the same image digests from staging to production after automated checks and an environment approval. Keep secrets out of image layers and source control. Run schema migrations as a one-off release task before shifting application traffic; migrations must remain backward-compatible with the currently running application during rolling deploys.

Production traffic terminates TLS at a managed load balancer/WAF, then reaches private web/API services. PostgreSQL, Redis, object storage, and monitoring are managed services in private networks. Use at least two availability zones, health checks, autoscaling from CPU/latency/queue depth, and separate database credentials with least privilege. Keep non-production data synthetic or irreversibly anonymized.

## AWS mapping

- Web and API containers: ECS Fargate behind an Application Load Balancer; separate services and task roles.
- Edge protection: Route 53, ACM TLS certificates, ALB, AWS WAF, private subnets and security groups.
- PostgreSQL: RDS PostgreSQL Multi-AZ with encryption, automated backups, point-in-time recovery, deletion protection and tested restore drills.
- Redis: ElastiCache for Redis in private subnets; do not store durable refresh-token state only in cache.
- Documents: private S3 buckets with KMS encryption, lifecycle policy, malware scanning and short-lived presigned URLs.
- Secrets/config: Secrets Manager or Parameter Store; JWT signing key in a KMS-backed rotation process.
- Observability: CloudWatch logs/alarms plus Prometheus-compatible metrics/Grafana or the organization's managed observability platform.
- Delivery: GitHub Actions OIDC to a narrowly scoped AWS role, publish SHA-tagged images to ECR, run a migration task, update ECS task definitions, wait for healthy deployment, and roll back to the prior task definition if health checks fail.

## Azure mapping

- Web and API containers: Azure Container Apps with separate apps/revisions and internal ingress for the API.
- Edge protection: Front Door/WAF, managed certificates, private endpoints and VNets.
- PostgreSQL: Azure Database for PostgreSQL Flexible Server with zone redundancy, backups/PITR and restore exercises.
- Redis: Azure Managed Redis in the private network.
- Documents: private Blob Storage with managed identity and short-lived delegated access.
- Secrets/config: Key Vault and workload identity; rotate signing keys without embedding them in deployments.
- Observability: Azure Monitor, Application Insights, managed Prometheus/Grafana.
- Delivery: GitHub Actions federated credentials, publish immutable images to ACR, deploy a new Container Apps revision, run health/smoke checks, shift traffic gradually, and retain the previous revision for rollback.

## Release gates

1. Lint, typecheck, backend unit/integration tests, migration validation against PostgreSQL, dependency/container scans, and image SBOM generation.
2. Deploy to staging, apply migrations, run API smoke tests and representative role/branch authorization tests.
3. Manual production approval, migrate with a backward-compatible change, then canary/rolling service deployment.
4. Verify error rate, latency, saturation, and business checks; roll back application revisions if thresholds regress. Database rollback is a forward-fix or restore decision, not an automatic destructive action.
5. Regular restore, key rotation, incident response, and access-review exercises.

The included Compose setup is for local development. Cloud resources, identities, secrets, domains, and production approvals are environment-specific and must be provisioned by the school's cloud administrators; no production account or credentials are assumed by this repository.