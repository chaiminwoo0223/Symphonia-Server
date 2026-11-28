variable "project_id" {
  description = "dev 전용 GCP 프로젝트 ID"
  type        = string
}

variable "region" {
  type    = string
  default = "asia-northeast3"
}

variable "zone" {
  type    = string
  default = "asia-northeast3-a"
}

variable "name" {
  description = "리소스 이름의 접두어. infra/scripts/common.sh의 NAME과 같아야 한다."
  type        = string
  default     = "mariajou-dev"
}

variable "subnet_cidr" {
  type    = string
  default = "10.10.0.0/24"
}

variable "db_tier" {
  description = "Cloud SQL 사양. 부하 테스트 때만 올린다. 바꾸면 DB가 다시 시작된다."
  type        = string
  default     = "db-f1-micro"
}

variable "db_disk_size_gb" {
  type    = number
  default = 10
}

variable "db_backup_enabled" {
  description = "Cloud SQL 자동 백업. dev는 비용과 잔여물을 줄이려고 끈다."
  type        = bool
  default     = false
}

variable "db_deletion_protection" {
  description = "DB를 실수로 지우지 않게 하는 보호. 완전 철거 스크립트만 false로 바꾼다."
  type        = bool
  default     = true
}

variable "db_password" {
  description = "DB 비밀번호. Secret Manager의 DB_PASSWORD 값을 스크립트가 TF_VAR_db_password로 넘긴다. 상태에 남지 않는다."
  type        = string
  sensitive   = true
  ephemeral   = true
}

variable "db_password_version" {
  description = "DB 비밀번호를 바꿀 때만 올린다. 값이 바뀌어야 새 비밀번호를 보낸다."
  type        = number
  default     = 1
}
