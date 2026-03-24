# LumineApp

Android AR jewelry try-on app — thesis project implementing the **ALBJOA** (Adaptive Landmark-Based Jewelry Overlay Algorithm) using a hybrid MediaPipe + Unity architecture.

---

## What it does

Users can browse a jewelry catalog and virtually try on pieces (rings, necklaces, earrings, bracelets) using their phone's front camera. MediaPipe detects hand and face landmarks in real time, and Unity renders a 3D jewelry model overlaid on the correct position on screen.

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose |
| Computer Vision | Google MediaPipe (HandLandmarker + FaceLandmarker) |
| 3D Rendering | Unity (exported as Android Library) |
| Backend | ASP.NET Core ([LumineBackend](https://github.com/VelzaNava/LumineBackend)) |
| Database & Auth | Supabase (PostgreSQL + GoTrue + Storage) |
| Language | Kotlin |

---

## Features

- **AR Try-On** — real-time 3D jewelry overlay using hand and face tracking
- **Jewelry Catalog** — browse and filter by category (rings, necklaces, earrings, bracelets)
- **Favorites** — save jewelry items for later
- **User Profile** — edit name, phone, upload profile picture
- **OTP Registration** — email verification before account creation
- **Terms & Conditions** — must scroll to bottom before agreeing
- **Evaluation System** — rate the AR try-on experience (1–5 stars) after each session
- **Admin Panel** — manage jewelry catalog and user accounts

---

## Architecture

```
Camera Frame
     ↓
MediaPipe (HandLandmarker / FaceLandmarker)
     ↓
UnityBridge (landmark coordinates as "x,y,z" string)
     ↓
UnitySendMessage → JewelryTracker.cs (Unity)
     ↓
3D Jewelry Rendered on Screen
```

The app also runs a 2D overlay (`AROverlayView`) in parallel as a lightweight fallback.

---

## Getting Started

### Requirements
- Android Studio Hedgehog or later
- Android device with API 26+ (Android 8.0)
- [LumineBackend](https://github.com/VelzaNava/LumineBackend) running locally or on a server

### Setup

1. Clone the repo:
```bash
git clone https://github.com/VelzaNava/LumineApp.git
```

2. Open in Android Studio

3. Update the backend URL in `RetrofitClient.kt`:
```kotlin
private const val BASE_URL = "http://your-backend-url:5111/"
```

4. Run on a physical device (camera required)

---

## Project Structure

```
app/src/main/java/com/thesis/lumine/
├── data/
│   ├── api/          # Retrofit API service + client
│   ├── model/        # Data classes (Jewelry, AuthModels, UserProfile)
│   └── repository/   # JewelryRepository — single source of truth
├── ui/
│   ├── auth/         # Login, Register, OTP verification screens
│   ├── ar/           # AR try-on screen + rating dialog
│   ├── catalog/      # Jewelry catalog screen
│   ├── profile/      # Profile, edit profile, favorites screens
│   └── admin/        # Admin panel (jewelry CRUD + user management)
├── utils/
│   ├── CameraManager.kt    # CameraX setup + frame preprocessing
│   ├── MediaPipeHelper.kt  # Hand and face landmark detection
│   ├── UnityBridge.kt      # Android ↔ Unity IPC via UnitySendMessage
│   ├── AROverlayView.kt    # 2D canvas overlay for jewelry drawing
│   └── SessionManager.kt  # Encrypted local session storage
└── viewmodel/        # AuthViewModel, ProfileViewModel, JewelryViewModel, EvaluationViewModel
```

---

## Backend

The REST API backend for this app is at:
[github.com/VelzaNava/LumineBackend](https://github.com/VelzaNava/LumineBackend)

---

## Thesis Context

This app is part of a thesis implementing the **ALBJOA** algorithm on a hybrid Android + Unity platform, evaluated using the **TAM3** (Technology Acceptance Model 3) framework. Tested on a Vivo Y100 (mid-range Android device).

---

