# TaxiVN Android — Hệ Thống Điều Hành Taxi (Kotlin & Jetpack Compose)

Dự án Android chính thức được xây dựng bằng **Kotlin**, **Jetpack Compose (Material 3)**, **Retrofit2**, và **Google Play Services FusedLocationProviderClient**.

## Tính năng chính
1. **Logo TaxiVN & Màn hình khởi động (Splash Screen API)**: `ic_taxivn_logo.xml`, `Theme.TaxiVN.Splash`, `SplashScreen.kt`.
2. **Đăng nhập phân quyền Tài xế & Điều hành viên**: `LoginScreen.kt` + `SessionPreferences.kt` (DataStore).
3. **Kết nối máy chủ TaxiVN hiện tại**: Mặc định trỏ tới `https://taxivn-h-th-ng-i-u-h-nh-taxi.ai.studio/` (hỗ trợ cấu hình URL API đồng hành, đồng bộ non-destructive).
4. **Định vị GPS & Foreground Service**: `LocationTracker.kt` + `GpsLocationService.kt` cập nhật tọa độ, tốc độ, sai số và trạng thái tài xế (`Sẵn Sàng`, `Đang Chở Khách`, `Chưa bật nhận khách`).
5. **Quy trình điều hành TaxiVN**: Tạo cuốc xe, gán tài xế, hủy cuốc, thanh toán công nợ tài xế, khóa/mở khóa tài xế, quy trình 3 bước chuyến đi.

## Hướng dẫn đóng gói & xuất file APK

### Cách 1: Bằng Android Studio (Khuyến nghị)
1. Giải nén thư mục dự án và mở bằng **Android Studio Ladybug / Meerkat** trở lên.
2. Đợi Gradle đồng bộ thư viện (`Gradle Sync`).
3. Chọn **Build > Build Bundle(s) / APK(s) > Build APK(s)** để xuất file `TaxiVN-1.0.0-debug.apk`.
4. Hoặc chọn **Build > Generate Signed Bundle / APK...** -> **APK** để xuất bản phát hành (`release`).

### Cách 2: Bằng dòng lệnh Gradle
```bash
# Xuất APK Debug cài đặt thử nghiệm ngay
./gradlew assembleDebug
# File đầu ra: app/build/outputs/apk/debug/TaxiVN-1.0.0-debug.apk

# Xuất APK Release (cần cấu hình biến môi trường keystore)
export TAXIVN_KEYSTORE_PATH=/duong-dan/taxivn-release.jks
export TAXIVN_KEYSTORE_PASSWORD=mat_khau_keystore
export TAXIVN_KEY_ALIAS=taxivn
export TAXIVN_KEY_PASSWORD=mat_khau_key
./gradlew assembleRelease
# File đầu ra: app/build/outputs/apk/release/TaxiVN-1.0.0-release.apk
```

### Cài đặt trên điện thoại Android
- Bật **Cài đặt ứng dụng không xác định (Install unknown apps)** trên điện thoại Android.
- Chép file `.apk` sang điện thoại hoặc cài qua ADB:
```bash
adb install app/build/outputs/apk/debug/TaxiVN-1.0.0-debug.apk
```
