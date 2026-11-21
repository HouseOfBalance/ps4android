use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fs::File;
use std::io::{Read, Seek, SeekFrom};
use std::path::Path;
use thiserror::Error;

#[derive(Error, Debug)]
pub enum SfoError {
    #[error("I/O error: {0}")]
    Io(#[from] std::io::Error),
    #[error("Invalid SFO magic signature")]
    InvalidMagic,
    #[error("Invalid SFO structure")]
    InvalidFormat,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GameMetadata {
    pub title_id: String,
    pub title: String,
    pub app_version: String,
    pub category: String,
    pub icon_path: Option<String>,
}

pub struct SfoParser;

impl SfoParser {
    pub fn parse<P: AsRef<Path>>(path: P) -> Result<HashMap<String, String>, SfoError> {
        let mut file = File::open(path)?;
        let mut magic = [0u8; 4];
        file.read_exact(&mut magic)?;

        // '\0PSF' = [0x00, 0x50, 0x53, 0x46]
        if magic != [0x00, 0x50, 0x53, 0x46] {
            return Err(SfoError::InvalidMagic);
        }

        let mut header = [0u8; 16];
        file.read_exact(&mut header)?;

        let key_table_offset = u32::from_le_bytes(
            header[4..8].try_into().map_err(|_| SfoError::InvalidFormat)?
        ) as u64;
        let data_table_offset = u32::from_le_bytes(
            header[8..12].try_into().map_err(|_| SfoError::InvalidFormat)?
        ) as u64;
        let num_entries = u32::from_le_bytes(
            header[12..16].try_into().map_err(|_| SfoError::InvalidFormat)?
        ) as usize;

        let mut entries = Vec::with_capacity(num_entries);
        for _ in 0..num_entries {
            let mut entry = [0u8; 16];
            file.read_exact(&mut entry)?;
            let key_offset = u16::from_le_bytes(
                entry[0..2].try_into().map_err(|_| SfoError::InvalidFormat)?
            ) as u64;
            let data_fmt = u16::from_le_bytes(
                entry[2..4].try_into().map_err(|_| SfoError::InvalidFormat)?
            );
            let data_len = u32::from_le_bytes(
                entry[4..8].try_into().map_err(|_| SfoError::InvalidFormat)?
            ) as usize;
            let data_offset = u32::from_le_bytes(
                entry[12..16].try_into().map_err(|_| SfoError::InvalidFormat)?
            ) as u64;
            entries.push((key_offset, data_fmt, data_len, data_offset));
        }

        let mut result = HashMap::new();
        for (key_off, _fmt, data_len, data_off) in entries {
            // Read Key
            file.seek(SeekFrom::Start(key_table_offset + key_off))?;
            let mut key_bytes = Vec::with_capacity(64);
            let mut b = [0u8; 1];
            while file.read_exact(&mut b).is_ok() && b[0] != 0 {
                key_bytes.push(b[0]);
                if key_bytes.len() > 256 {
                    return Err(SfoError::InvalidFormat); // Prevent OOM on malformed files
                }
            }
            let key = String::from_utf8_lossy(&key_bytes).to_string();

            // Read Value
            file.seek(SeekFrom::Start(data_table_offset + data_off))?;
            let mut val_bytes = vec![0u8; data_len];
            file.read_exact(&mut val_bytes)?;
            
            // Trim null terminators if any
            let val = String::from_utf8_lossy(&val_bytes)
                .trim_matches('\0')
                .to_string();

            result.insert(key, val);
        }

        Ok(result)
    }

    pub fn extract_metadata<P: AsRef<Path>>(dir_path: P) -> Option<GameMetadata> {
        let dir = dir_path.as_ref();
        let sfo_path = dir.join("sce_sys").join("param.sfo");
        if !sfo_path.exists() {
            return None;
        }

        let map = Self::parse(&sfo_path).ok()?;
        let icon_path = dir.join("sce_sys").join("icon0.png");

        Some(GameMetadata {
            title_id: map.get("TITLE_ID").cloned().unwrap_or_else(|| "UNKNOWN".into()),
            title: map.get("TITLE").cloned().unwrap_or_else(|| "Unknown Title".into()),
            app_version: map.get("APP_VER").cloned().unwrap_or_else(|| "1.00".into()),
            category: map.get("CATEGORY").cloned().unwrap_or_else(|| "gdr".into()),
            icon_path: if icon_path.exists() {
                Some(icon_path.to_string_lossy().to_string())
            } else {
                None
            },
        })
    }
}
