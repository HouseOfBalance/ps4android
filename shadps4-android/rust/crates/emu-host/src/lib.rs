pub mod ffi_core;
use shadps4_driver_mgr::DriverManager;
use shadps4_library::SfoParser;
use std::ffi::CString;
use std::fs;
use std::path::Path;
use std::ptr;

uniffi::include_scaffolding!("shadps4");

#[derive(Debug, Clone)]
pub struct GameItem {
    pub title_id: String,
    pub title: String,
    pub app_version: String,
    pub category: String,
    pub icon_path: Option<String>,
    pub folder_path: String,
}

#[derive(Debug, Clone)]
pub struct DriverItem {
    pub name: String,
    pub description: String,
    pub author: String,
    pub driver_version: String,
    pub library_name: String,
}

pub fn init_android_logging() {
    #[cfg(target_os = "android")]
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Trace)
            .with_tag("ShadPS4RustHost"),
    );
}

pub fn scan_game_folder(folder_path: String) -> Option<GameItem> {
    let p = Path::new(&folder_path);
    let meta = SfoParser::extract_metadata(p)?;
    Some(GameItem {
        title_id: meta.title_id,
        title: meta.title,
        app_version: meta.app_version,
        category: meta.category,
        icon_path: meta.icon_path,
        folder_path,
    })
}

pub fn scan_game_directory(root_directory: String) -> Vec<GameItem> {
    let mut games = Vec::new();
    let root = Path::new(&root_directory);
    if let Ok(entries) = fs::read_dir(root) {
        for entry in entries.flatten() {
            let path = entry.path();
            if path.is_dir() {
                if let Some(game) = scan_game_folder(path.to_string_lossy().to_string()) {
                    games.push(game);
                }
            }
        }
    }
    games
}

pub fn get_installed_drivers(storage_dir: String) -> Vec<DriverItem> {
    let mgr = DriverManager::new(storage_dir);
    mgr.list_installed_drivers()
        .into_iter()
        .map(|d| DriverItem {
            name: d.name,
            description: d.description,
            author: d.author,
            driver_version: d.driver_version,
            library_name: d.library_name,
        })
        .collect()
}

pub fn install_driver(storage_dir: String, zip_path: String) -> bool {
    let mgr = DriverManager::new(storage_dir);
    mgr.install_driver_zip(zip_path).is_ok()
}

static IS_EMU_RUNNING: std::sync::atomic::AtomicBool = std::sync::atomic::AtomicBool::new(false);

pub fn start_emulation(
    game_path: String,
    title_id: String,
    custom_driver_path: Option<String>,
    enable_tso: bool,
) -> i32 {
    use std::sync::atomic::Ordering;

    if IS_EMU_RUNNING.swap(true, Ordering::SeqCst) {
        log::warn!("Emulation already running! Ignoring start request.");
        return 0;
    }

    let c_game_path = CString::new(game_path).unwrap_or_default();
    let c_title_id = CString::new(title_id).unwrap_or_default();
    let c_driver_path = custom_driver_path.and_then(|p| CString::new(p).ok());

    let config = ffi_core::EmuConfig {
        game_path: c_game_path.as_ptr(),
        title_id: c_title_id.as_ptr(),
        custom_driver_path: c_driver_path
            .as_ref()
            .map(|c| c.as_ptr())
            .unwrap_or(ptr::null()),
        enable_tso,
        enable_vector_tso: false,
        enable_gpl: true,
        native_window: ptr::null_mut(),
        state_cb: None,
    };

    unsafe {
        let res = ffi_core::shadps4_core_init(&config);
        if res == 0 {
            std::thread::spawn(|| {
                ffi_core::shadps4_core_run();
                IS_EMU_RUNNING.store(false, Ordering::SeqCst);
            });
        } else {
            IS_EMU_RUNNING.store(false, Ordering::SeqCst);
        }
        res
    }
}

pub fn stop_emulation() -> i32 {
    use std::sync::atomic::Ordering;
    IS_EMU_RUNNING.store(false, Ordering::SeqCst);
    unsafe { ffi_core::shadps4_core_stop() }
}

pub fn update_controller_pad(
    buttons: i32,
    lx: f32,
    ly: f32,
    rx: f32,
    ry: f32,
    l2: f32,
    r2: f32,
) {
    let pad = ffi_core::NativePadData {
        buttons: buttons as u32,
        left_stick_x: lx,
        left_stick_y: ly,
        right_stick_x: rx,
        right_stick_y: ry,
        l2_trigger: l2,
        r2_trigger: r2,
    };
    unsafe {
        ffi_core::shadps4_input_update_pad(0, &pad);
    }
}

pub fn update_emulation_surface(surface_handle: i64, width: i32, height: i32) {
    unsafe {
        ffi_core::shadps4_core_update_surface(surface_handle as *mut _, width, height);
    }
}

