variable "region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Name prefix for all resources"
  type        = string
  default     = "shopflow"
}

variable "cluster_version" {
  description = "Kubernetes version for EKS"
  type        = string
  default     = "1.30"
}

variable "db_username" {
  description = "Master username for the RDS PostgreSQL database"
  type        = string
  default     = "shopflow"
}

variable "db_password" {
  description = "Master password for the RDS database. Set this with TF_VAR_db_password, do not commit it."
  type        = string
  sensitive   = true
}
