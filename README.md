# ps4android (shadPS4 for Android)

Port của emulator PlayStation 4 (**shadPS4**) dành cho hệ điều hành **Android** (Target: SoC ARMv9 Snapdragon 8 Gen 2/3/Elite, GPU Adreno 7xx/8xx, Android 14+).

---

## 🏛️ Kiến Trúc Hệ Thống (Architecture)

```text
┌────────────────────────────────────────────────────────┐
│        Android Presentation (Kotlin / Jetpack Compose) │  <- UI Material 3, Touch Overlay, SAF
└───────────────────────────▲────────────────────────────┘
                            │ Mozilla UniFFI (RustBuffer)
┌───────────────────────────▼────────────────────────────┐
│         Backend Host Service (Rust cdylib)             │  <- SFO Parser, Driver Manager, State Guard
└───────────────────────────▲────────────────────────────┘
                            │ extern "C" (C-ABI Bridge)
┌───────────────────────────▼────────────────────────────┐
│      Native Core & Emulation Engine (C++20 / FEXCore)  │  <- Vulkan, Turnip Loader, AAudio, Memory Patch
└────────────────────────────────────────────────────────┘
```

## 📦 Cấu Trúc Dự Án

* **`shadps4-android/`**: Ứng dụng Android hoàn chỉnh
  * `app/`: Giao diện Kotlin Jetpack Compose (MainActivity, OnScreenControllerOverlay, và `EmulatorActivity` chạy riêng trên `android:process=":emu"`).
  * `rust/`: Backend Host Service bằng Rust (`crates/library`, `crates/driver-mgr`, `crates/emu-host`).
  * `native/`: Tầng Native C-ABI bridge, FEXCore x86 Trampoline Stubs, nạp driver Turnip (`libadrenotools`), Low-latency AAudio sink, và Vulkan Surface.
* **`shadPS4/`** (Submodule - Nhánh `android-port`):
  * Lõi emulator C++20 của shadPS4.
  * Đã patch Virtual Address Space < 512 GiB (`USER_MAX = 0x5FFFFFFFFFULL`).
  * Đã patch macro `HOST_CALL` sang ARM64 AAPCS64 tương thích FEXCore.
* **`shadPS4-launcher/`** (Submodule):
  * Desktop Tauri frontend gốc để tham khảo.

---

## 🛠️ Hướng Dẫn Build

Xem chi tiết từng bước cài đặt và biên dịch tại [BUILD_GUIDE.md](shadps4-android/BUILD_GUIDE.md).

## 📄 License

* `shadPS4` tuân theo giấy phép **GPL-2.0-or-later**.
