# k6 부하 생성기는 테스트할 때만 만들고 바로 지운다. 앱과 CPU를 나눠 쓰지 않게 별도 VM에서 실행한다.
terraform {
  required_version = ">= 1.11"

  backend "gcs" {
    prefix = "loadgen"
  }

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 7.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}

data "terraform_remote_state" "persistent" {
  backend = "gcs"

  config = {
    bucket = "${var.project_id}-tfstate"
    prefix = "persistent"
  }
}

resource "google_compute_instance" "k6" {
  name         = "${var.name}-k6"
  machine_type = var.machine_type
  tags         = ["ssh-from-iap"]

  boot_disk {
    auto_delete = true

    initialize_params {
      image = "projects/ubuntu-os-cloud/global/images/family/ubuntu-2404-lts-amd64"
      size  = 20
      type  = "pd-balanced"
    }
  }

  network_interface {
    subnetwork = data.terraform_remote_state.persistent.outputs.subnet_id

    # 패키지 설치에 인터넷이 필요하다. 들어오는 연결은 IAP SSH만 방화벽이 허용한다.
    access_config {}
  }

  metadata = {
    enable-oslogin = "TRUE"
  }

  metadata_startup_script = file("${path.module}/startup.sh")
}
