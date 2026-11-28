output "subnet_id" {
  value = google_compute_subnetwork.main.id
}

output "network_id" {
  value = google_compute_network.main.id
}

output "app_ip" {
  value = google_compute_address.app.address
}

output "repository_id" {
  value = google_artifact_registry_repository.app.repository_id
}

output "image_repository" {
  value = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.app.repository_id}/symphonia"
}

output "secret_ids" {
  value = [for secret in google_secret_manager_secret.app : secret.secret_id]
}

output "db_url" {
  value = "jdbc:postgresql://${google_sql_database_instance.main.public_ip_address}:5432/${google_sql_database.app.name}?sslmode=verify-ca&sslrootcert=/app/certs/server-ca.pem"
}

output "db_username" {
  value = google_sql_user.app.name
}

# 서버 인증서를 검증하는 공개 인증서라 비밀값이 아니다.
output "db_server_ca_cert" {
  value     = google_sql_database_instance.main.server_ca_cert[0].cert
  sensitive = true
}
