terraform {
  # 상태에 비밀번호를 남기지 않는 write-only 인자(password_wo)는 1.11부터 쓸 수 있다.
  required_version = ">= 1.11"

  backend "gcs" {
    # bucket은 프로젝트마다 달라 init할 때 -backend-config로 넘긴다.
    prefix = "persistent"
  }

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 7.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}
