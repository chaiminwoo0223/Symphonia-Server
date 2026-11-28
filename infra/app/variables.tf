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
  type    = string
  default = "mariajou-dev"
}

variable "machine_type" {
  type    = string
  default = "e2-small"
}

variable "disk_size_gb" {
  type    = number
  default = 30
}

variable "swap_size_gb" {
  type    = number
  default = 4
}
