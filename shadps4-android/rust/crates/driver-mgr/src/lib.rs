use serde::{Deserialize, Serialize};
use std::fs::File;
use std::io::Read;
use std::path::{Path, PathBuf};
use thiserror::Error;

#[derive(Error, Debug)]
pub enum DriverError {
    #[error("I/O error: {0}")]
    Io(#[from] std::io::Error),
    #[error("Zip extraction error: {0}")]
    Zip(#[from] zip::result::ZipError),
    #[error("Invalid driver package: missing meta.json")]
    MissingMeta,
    #[error("JSON parse error: {0}")]
    Json(#[from] serde_json::Error),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DriverMeta {
    pub name: String,
    pub description: String,
    pub author: String,
    pub package_version: String,
    pub driver_version: String,
    pub min_api: Option<u32>,
    pub library_name: String,
}

pub struct DriverManager {
    storage_dir: PathBuf,
}

impl DriverManager {
    pub fn new<P: AsRef<Path>>(storage_dir: P) -> Self {
        Self {
            storage_dir: storage_dir.as_ref().to_path_buf(),
        }
    }

    pub fn install_driver_zip<P: AsRef<Path>>(&self, zip_path: P) -> Result<DriverMeta, DriverError> {
        let file = File::open(zip_path)?;
        let mut archive = zip::ZipArchive::new(file)?;

        // Find and parse meta.json
        let mut meta_str = String::new();
        {
            let mut meta_file = archive.by_name("meta.json").map_err(|_| DriverError::MissingMeta)?;
            meta_file.read_to_string(&mut meta_str)?;
        }
        let meta: DriverMeta = serde_json::from_str(&meta_str)?;

        // Extract to target folder: storage_dir/drivers/<name>
        let target_dir = self.storage_dir.join("drivers").join(&meta.name);
        std::fs::create_dir_all(&target_dir)?;

        for i in 0..archive.len() {
            let mut file = archive.by_index(i)?;
            let outpath = match file.enclosed_name() {
                Some(path) => {
                    let full_path = target_dir.join(path);
                    // Zip Slip Path Traversal Protection
                    if !full_path.starts_with(&target_dir) {
                        continue;
                    }
                    full_path
                }
                None => continue,
            };

            if file.name().ends_with('/') {
                std::fs::create_dir_all(&outpath)?;
            } else {
                if let Some(p) = outpath.parent() {
                    if !p.exists() {
                        std::fs::create_dir_all(p)?;
                    }
                }
                let mut outfile = File::create(&outpath)?;
                std::io::copy(&mut file, &mut outfile)?;
            }
        }

        Ok(meta)
    }

    pub fn list_installed_drivers(&self) -> Vec<DriverMeta> {
        let drivers_dir = self.storage_dir.join("drivers");
        let mut list = Vec::new();
        if let Ok(entries) = std::fs::read_dir(drivers_dir) {
            for entry in entries.flatten() {
                let meta_file = entry.path().join("meta.json");
                if meta_file.exists() {
                    if let Ok(content) = std::fs::read_to_string(meta_file) {
                        if let Ok(meta) = serde_json::from_str(&content) {
                            list.push(meta);
                        }
                    }
                }
            }
        }
        list
    }
}
