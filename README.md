# 🕋 Adhan Android

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4.svg?style=flat&logo=google-chrome)](https://developer.android.com/jetpack/compose)
[![Hilt](https://img.shields.io/badge/Dagger-Hilt-yellow.svg?style=flat)](https://dagger.dev/hilt/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)

A modern, highly customizable Adhan application built with Jetpack Compose. Providing precise prayer times based on the Ahmadiyya calculation method and many others, featuring a dynamic sky visualizer and advanced audio routing.

---

## 🍎 Sister Project (iOS)

Check out the iOS version of this project at: **[adhan-ios](https://github.com/4islam/adhan-ios)**

---

## ✨ Features

### 🕒 Precise Prayer Timing

- **Multiple Calculation Methods**: Ahmadiyya (Default), MWL, ISNA, Egypt, Makkah, Karachi, Tehran, Jafari, and Gulf.
- **High Latitude Support**: Robust rules for regions with extreme timings (Angle-based, Mid-night, One-seventh).
- **Manual Offsets**: Fine-tune individual prayer times by +/- minutes.
- **Combining Logic**: Specialized logic for combining prayers (Maghrib/Isha) during short nights or specific thresholds.

### 🌌 Sky Visualizer & Moon Phases

- **Dynamic Skylight**: A visual representation of the sky state (Sunrise, Noon, Sunset, Night).
- **Time Scrubbing**: Travel through the day to see how the sky changes.
- **Astro Events**: Track Solar Noon, Midnight, and astronomical events.
- **Moon Phases**: Integrated moon phase calculation and visualization.

### 🔊 Advanced Audio Engine

- **Custom Adhan Sounds**: Select different Adhan audio for each prayer.
- **Audio Fading**: Professional volume fading (fade-in/fade-out) for a pleasant experience.
- **Smart Routing**: Direct audio to specific output devices (Bluetooth, System, etc.).
- **Volume Control**: Per-day and per-prayer volume customization.

### 🛠 Reliability & Diagnostics

- **Event Tracking**: A full history of Adhan events (Scheduled, Triggered, Playing, Success, Error).
- **Log Management**: Built-in log viewer with automatic rotation and sharing capabilities.
- **System Health**: Checks for Battery Optimization, Exact Alarm permissions, and Notification status.

## 🏗 Technology Stack

- **UI**: 100% Jetpack Compose for a modern, fluid user interface.
- **Architecture**: Clean Architecture with MVI/MVVM patterns.
- **Dependency Injection**: Hilt (Dagger) for robust and testable code.
- **Media**: Media3 (ExoPlayer) for reliable audio playback.
- **Concurrency**: Kotlin Coroutines and Flow for reactive data streams.
- **Navigation**: Type-safe Compose Navigation.

## 🚀 Getting Started

1. Clone the repository.
2. Open in Android Studio (Ladybug or newer recommended).
3. Ensure you have the latest Android SDK and Gradle version.
4. Build and run on a physical device or emulator.

## 📜 Credits

This project utilizes the core logic and formulas from the [PrayTime.js](http://tanzil.info/praytime) library, originally developed by **Hamid Zarrabi-Zadeh**.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

*Built with ❤️ for the global Muslim community.*
