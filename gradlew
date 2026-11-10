#!/bin/sh
# TaxiVN Gradle Wrapper launcher script (Unix / macOS)
DIRNAME="$(dirname "$0")"
if [ -x "$(command -v gradle)" ]; then
  exec gradle "$@"
else
  echo "Vui lòng mở dự án trong Android Studio hoặc cài đặt Gradle Wrapper JAR bằng lệnh: gradle wrapper"
  exit 1
fi
