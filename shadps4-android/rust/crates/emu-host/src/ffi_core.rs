use std::os::raw::{c_char, c_int, c_void};

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub enum EmuState {
    Stopped = 0,
    Starting = 1,
    Running = 2,
    Paused = 3,
    Crashed = 4,
}

pub type EmuStateCallback = extern "C" fn(state: EmuState, message: *const c_char);

#[repr(C)]
pub struct EmuConfig {
    pub game_path: *const c_char,
    pub title_id: *const c_char,
    pub custom_driver_path: *const c_char,
    pub enable_tso: bool,
    pub enable_vector_tso: bool,
    pub enable_gpl: bool,
    pub native_window: *mut c_void,
    pub state_cb: Option<EmuStateCallback>,
}

extern "C" {
    pub fn shadps4_core_init(config: *const EmuConfig) -> c_int;
    pub fn shadps4_core_run() -> c_int;
    pub fn shadps4_core_pause() -> c_int;
    pub fn shadps4_core_resume() -> c_int;
    pub fn shadps4_core_stop() -> c_int;
    pub fn shadps4_core_update_surface(
        native_window: *mut c_void,
        width: i32,
        height: i32,
    ) -> c_int;
    pub fn shadps4_input_update_pad(pad_index: c_int, data: *const NativePadData);
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct NativePadData {
    pub buttons: u32,
    pub left_stick_x: f32,
    pub left_stick_y: f32,
    pub right_stick_x: f32,
    pub right_stick_y: f32,
    pub l2_trigger: f32,
    pub r2_trigger: f32,
}
