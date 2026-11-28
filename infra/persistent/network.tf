locals {
  services = [
    "artifactregistry.googleapis.com",
    "compute.googleapis.com",
    "iap.googleapis.com",
    "secretmanager.googleapis.com",
    "sqladmin.googleapis.com",
  ]
}

resource "google_project_service" "required" {
  for_each = toset(local.services)

  service = each.value
  # 켜 둔 API는 비용이 없고, 끄기는 남은 의존 때문에 destroy를 실패시킬 수 있어 그대로 둔다.
  disable_on_destroy = false
}

# default 네트워크는 SSH를 모든 주소에 열어 두므로 전용 VPC를 쓴다.
resource "google_compute_network" "main" {
  name                    = "${var.name}-vpc"
  auto_create_subnetworks = false

  depends_on = [google_project_service.required]
}

resource "google_compute_subnetwork" "main" {
  name          = "${var.name}-subnet"
  network       = google_compute_network.main.id
  region        = var.region
  ip_cidr_range = var.subnet_cidr
}

resource "google_compute_firewall" "ssh_from_iap" {
  name    = "${var.name}-ssh-from-iap"
  network = google_compute_network.main.id

  # IAP TCP 전달이 쓰는 대역이다.
  source_ranges = ["35.235.240.0/20"]
  target_tags   = ["ssh-from-iap"]

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }
}

# DuckDNS 레코드와 Cloud SQL 허용 목록이 이 주소를 가리키므로, VM을 다시 만들어도 남게 app 모듈과 분리한다.
resource "google_compute_address" "app" {
  name   = "${var.name}-app-ip"
  region = var.region

  depends_on = [google_project_service.required]
}
