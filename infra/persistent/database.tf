# 지운 인스턴스의 이름은 한동안 다시 쓸 수 없을 수 있어, 완전 철거 뒤 바로 다시 만들 수 있게 접미사를 붙인다.
resource "random_id" "db_suffix" {
  byte_length = 2
}

resource "google_sql_database_instance" "main" {
  name             = "${var.name}-db-${random_id.db_suffix.hex}"
  region           = var.region
  database_version = "POSTGRES_17"

  deletion_protection = var.db_deletion_protection

  settings {
    # PostgreSQL 16 이상의 기본 에디션은 ENTERPRISE_PLUS라서 공유 코어 사양을 쓰려면 명시해야 한다.
    edition           = "ENTERPRISE"
    tier              = var.db_tier
    availability_type = "ZONAL"
    disk_type         = "PD_SSD"
    disk_size         = var.db_disk_size_gb
    disk_autoresize   = false

    # 완전 철거 뒤에 백업이 남지 않게 한다.
    retain_backups_on_delete = false

    final_backup_config {
      enabled = false
    }

    backup_configuration {
      enabled = var.db_backup_enabled
    }

    # 사설 IP 연결은 인스턴스를 지운 뒤에도 VPC 피어링이 남아 삭제를 막을 수 있어, 공인 IP를 앱 VM에만 연다.
    ip_configuration {
      ipv4_enabled = true
      ssl_mode     = "ENCRYPTED_ONLY"

      authorized_networks {
        name  = "app-vm"
        value = "${google_compute_address.app.address}/32"
      }
    }
  }

  depends_on = [google_project_service.required]
}

resource "google_sql_database" "app" {
  name     = "symphonia"
  instance = google_sql_database_instance.main.name

  # 객체가 남은 데이터베이스와 소유자는 따로 지우면 실패한다. 인스턴스를 지울 때 함께 사라지므로 상태에서만 뺀다.
  deletion_policy = "ABANDON"
}

resource "google_sql_user" "app" {
  name                = "symphonia"
  instance            = google_sql_database_instance.main.name
  password_wo         = var.db_password
  password_wo_version = var.db_password_version
  deletion_policy     = "ABANDON"
}
