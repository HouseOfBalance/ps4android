# shadPS4 Android - Hướng dẫn Cài đặt & Build Hệ Thống

Tài liệu này cung cấp toàn bộ quy trình thiết lập môi trường để biên dịch ứng dụng Android (Kotlin + Rust + C++ Native Core).

---

## 1. Yêu cầu Công cụ (Prerequisites)

1. **Android Studio**: Ladybug / Koala (hoặc phiên bản 2024.1+ trở lên)
   - Android SDK 35
   - Android NDK: `r27b` hoặc `r28`
   - CMake 3.22.1+ (cài qua Android SDK Manager)
2. **Rust Toolchain**:
   ```bash
   curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
   rustup target add aarch64-linux-android
   cargo install cargo-ndk uniffi-bindgen
   ```
3. **Java JDK**: Eclipse Temurin hoặc OpenJDK 17

---

## 2. Cấu trúc Dự án

```text
shadps4-project/
├── shadPS4/                       <- Fork Core C++ (nhánh android-port)
│   └── src/core/address_space.cpp (Đã patch < 512 GiB VA limit cho Android)
├── shadps4-android/               <- Android Client & Native Host
│   ├── app/                       <- Kotlin Jetpack Compose UI
│   │   ├── MainActivity.kt        <- Giao diện thư viện game
│   │   ├── EmulatorActivity.kt    <- Process :emu độc lập
│   │   └── ui/DriverManagerScreen.kt <- Giao diện cài driver Turnip
│   ├── rust/                      <- Rust Backend
│   │   ├── crates/library         <- SFO / Metadata parser
│   │   ├── crates/driver-mgr      <- Quản lý zip Turnip driver
│   │   └── crates/emu-host        <- UniFFI binding & C-ABI Controller
│   └── native/                    <- C++ Bridge & Graphics
│       ├── bridge/                <- shadps4_core_c_abi (.h/.cpp)
│       ├── fex-frontend/          <- FEXCore ARM64 JIT integration
│       ├── driver-loader/         <- libadrenotools wrapper nạp Turnip
│       └── vulkan-surface/        <- ANativeWindow + VK_KHR_android_surface
```

---

## 3. Các bước Build Dự án

### Bước 1: Build Rust cdylib (`libshadps4_emu_host.so`)
Tại thư mục `shadps4-android/rust`:
```bash
cargo ndk -t arm64-v8a -o ../app/src/main/jniLibs build --release
```

### Bước 2: Sinh UniFFI Kotlin Bindings
```bash
cargo run --bin uniffi-bindgen generate src/shadps4.udl --language kotlin --out-dir ../app/src/main/java/net/shadps4/android/ui
```

### Bước 3: Build & Chạy APK
Mở thư mục `shadps4-android` trong **Android Studio**:
- Kết nối điện thoại chip Snapdragon (khuyên dùng Snap 8 Gen 2 / 8 Gen 3 / Elite).
- Nhấn **Run 'app'** hoặc gõ lệnh terminal:
  ```bash
  ./gradlew assembleDebug
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

---

## 4. Cách sử dụng trên Điện thoại
1. Mở ứng dụng **shadPS4 Android**.
2. Nhấn nút **Drivers** -> chọn **Install .ZIP** để cài driver Mesa Turnip tương ứng với GPU Adreno của máy.
3. Nhấn **Select Game Directory** để chọn thư mục chứa game PS4 đã giải nén (gồm `sce_sys/param.sfo` và file thực thi).
4. Nhấn vào bìa game để khởi chạy môi trường giả lập trên tiến trình `:emu` độc lập.
