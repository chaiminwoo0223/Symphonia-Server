resource "google_artifact_registry_repository" "app" {
  repository_id = var.name
  location      = var.region
  format        = "DOCKER"

  cleanup_policy_dry_run = false

  # 최근 5개는 남기고, 하루가 지난 나머지는 지운다. KEEP이 DELETE보다 우선한다.
  cleanup_policies {
    id     = "keep-recent"
    action = "KEEP"

    most_recent_versions {
      keep_count = 5
    }
  }

  cleanup_policies {
    id     = "delete-old"
    action = "DELETE"

    condition {
      tag_state  = "ANY"
      older_than = "86400s"
    }
  }

  depends_on = [google_project_service.required]
}

# 값은 상태에 남지 않도록 Terraform 밖(scripts/secrets.sh)에서 버전으로 등록한다.
resource "google_secret_manager_secret" "app" {
  for_each = toset(compact(split("\n", file("${path.module}/../secrets.txt"))))

  secret_id = each.value

  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }

  depends_on = [google_project_service.required]
}
