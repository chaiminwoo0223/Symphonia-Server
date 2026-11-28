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
  description = "부하 생성기가 병목이 되지 않게 전용 vCPU 사양을 쓴다."
  type        = string
  default     = "e2-standard-2"
}
