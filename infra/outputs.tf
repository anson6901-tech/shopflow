output "region" {
  value = var.region
}

output "cluster_name" {
  value = module.eks.cluster_name
}

output "ecr_repository_urls" {
  description = "ECR repository URLs, one per service"
  value       = { for k, r in aws_ecr_repository.service : k => r.repository_url }
}

output "rds_endpoint" {
  description = "Host:port of the database. Use the host part in the DB URL."
  value       = module.rds.db_instance_endpoint
}

output "rds_database_name" {
  value = "shopflow"
}

output "order_events_queue_name" {
  value = aws_sqs_queue.order_events.name
}

output "order_events_queue_url" {
  value = aws_sqs_queue.order_events.url
}
