terraform {
  required_version = ">= 1.11"

  backend "gcs" {
    prefix = "app"
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

locals {
  persistent = data.terraform_remote_state.persistent.outputs
}

resource "google_service_account" "app" {
  account_id   = "${var.name}-app"
  display_name = "${var.name} app VM"
}

resource "google_artifact_registry_repository_iam_member" "pull" {
  repository = local.persistent.repository_id
  location   = var.region
  role       = "roles/artifactregistry.reader"
  member     = google_service_account.app.member
}

resource "google_secret_manager_secret_iam_member" "read" {
  for_each = toset(local.persistent.secret_ids)

  secret_id = each.value
  role      = "roles/secretmanager.secretAccessor"
  member    = google_service_account.app.member
}

# 앱 포트(8080)와 management 포트(8081)는 열지 않는다. 외부 요청은 Caddy만 받는다.
resource "google_compute_firewall" "web" {
  name    = "${var.name}-web"
  network = local.persistent.network_id

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["web"]

  allow {
    protocol = "tcp"
    ports    = ["80", "443"]
  }
}

resource "google_compute_instance" "app" {
  name         = "${var.name}-app"
  machine_type = var.machine_type
  tags         = ["ssh-from-iap", "web"]

  # 머신 타입을 바꿀 때 Terraform이 VM을 중지했다가 다시 시작할 수 있게 한다.
  allow_stopping_for_update = true

  boot_disk {
    auto_delete = true

    initialize_params {
      image = "projects/ubuntu-os-cloud/global/images/family/ubuntu-2404-lts-amd64"
      size  = var.disk_size_gb
      type  = "pd-balanced"
    }
  }

  network_interface {
    subnetwork = local.persistent.subnet_id

    access_config {
      nat_ip = local.persistent.app_ip
    }
  }

  service_account {
    email  = google_service_account.app.email
    scopes = ["cloud-platform"]
  }

  metadata = {
    enable-oslogin = "TRUE"
  }

  metadata_startup_script = templatefile("${path.module}/startup.sh.tftpl", {
    swap_size_gb = var.swap_size_gb
  })

  depends_on = [
    google_artifact_registry_repository_iam_member.pull,
    google_secret_manager_secret_iam_member.read,
  ]
}
