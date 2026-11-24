use crate::config::LauncherConfig;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(default)]
pub struct BackupEnvelope {
    pub brand: String,
    pub app: String,
    pub developer: String,
    pub schema_version: i32,
    pub config: LauncherConfig,
    pub dock_packages: Vec<String>,
    pub home_items_json: String,
    pub folders_json: String,
}

impl Default for BackupEnvelope {
    fn default() -> Self {
        Self {
            brand: "SAROTECH".to_string(),
            app: "Advance Launcher".to_string(),
            developer: "Prashant Latta".to_string(),
            schema_version: 3,
            config: LauncherConfig::default(),
            dock_packages: Vec::new(),
            home_items_json: "[]".to_string(),
            folders_json: "[]".to_string(),
        }
    }
}

#[derive(Debug, Serialize)]
struct ValidationResponse {
    ok: bool,
    error: String,
    normalized_backup: Option<BackupEnvelope>,
}

pub fn validate_backup_json(raw: &str) -> String {
    let parsed: Result<BackupEnvelope, _> = serde_json::from_str(raw);
    let response = match parsed {
        Ok(mut env) => {
            env.config = env.config.normalized();
            env.brand = "SAROTECH".to_string();
            env.app = "Advance Launcher".to_string();
            env.developer = "Prashant Latta".to_string();
            env.schema_version = 3;
            ValidationResponse {
                ok: true,
                error: String::new(),
                normalized_backup: Some(env),
            }
        }
        Err(e) => ValidationResponse {
            ok: false,
            error: format!("Invalid Advance Launcher backup JSON: {}", e),
            normalized_backup: None,
        },
    };
    serde_json::to_string(&response)
        .unwrap_or_else(|_| "{"ok":false,"error":"Serialization failure"}".to_string())
}
