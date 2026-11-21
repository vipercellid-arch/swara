# Swara v4.8.8 — Pemutar Musik Modern & Swara Cloud

**Swara** adalah aplikasi pemutar musik Android modern berbasis **Kotlin + Jetpack Compose + Media3/ExoPlayer** yang dirancang halus, responsif di HP maupun **Tablet/Landscape**, dan terhubung langsung ke **Swara Cloud Backend** milik sendiri.

Dibuat dan dikembangkan oleh **Teves** ([@t3ves_](https://www.instagram.com/t3ves_?evlk=MXJuZHQybmxjMTV4NA==)).

---

## Fitur Utama Swara v4.8.8

- **Streaming Musik Cepat & Lirik Sinkron**: Mendukung katalog lagu lengkap, lirik sinkron (LRCLIB, SimpMusic, Better Lyrics), equalizer, serta pemutaran latar belakang stabil.
- **Swara Cloud Account & Sinkronisasi**: Login/Daftar akun Swara maupun **Masuk Cepat via Google** yang tersimpan di server pribadi **Swara Cloud** (`SQLite` permanen), sehingga statistik dengar, lagu disukai, dan playlist tidak pernah hilang saat ganti perangkat.
- **Auto-Update Otomatis**: Pengecekan versi otomatis langsung ke server **Swara Cloud** & GitHub Releases `vipercellid-arch/swara`.
- **Dukungan Penuh Tablet & Mode Landscape**: Tata letak adaptif dua panel (2-pane Settings, profil landscape, dan pemutar layar penuh).
- **Widget Layar Beranda**: Tersedia langsung melalui menu Widget Android tanpa memenuhi tab di dalam aplikasi.
- **Dukung Swara (Donasi QRIS)**: Halaman dukungan khusus terintegrasi langsung di dalam aplikasi.

---

## Struktur Repositori

- `app/` — Kode sumber aplikasi Android **Swara** (`com.swara.music`).
- `swara-cloud-server/` — Server backend mandiri **Swara Cloud** (Python 3 + SQLite) lengkap dengan skrip instalasi otomatis `setup-swara-cloud.sh`.
- `.github/workflows/build.yml` — Pipeline otomatis GitHub Actions untuk membangun `Swara-v4.8.8.apk`.

---

## Menjalankan Swara Cloud Backend di VPS

```bash
chmod +x swara-cloud-server/setup-swara-cloud.sh && sudo ./swara-cloud-server/setup-swara-cloud.sh
```

Endpoint aktif di port `3600`:
- `GET /health` — Status kesehatan server & jumlah pengguna.
- `GET /api/app/version` — Cek pembaruan otomatis aplikasi Swara.
- `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/google` — Autentikasi akun Swara & Masuk Cepat Google.
- `GET/POST /api/sync/stats` — Sinkronisasi data dengar & koleksi lagu ke cloud.
- `POST /api/rooms/*` — Fitur Dengar Bersama real-time.

---

## Lisensi & Atribusi Open-Source

Proyek ini dirilis di bawah lisensi **GNU General Public License v3.0 (GPL-3.0)** — lihat file [`LICENSE`](LICENSE).

Swara dibangun dengan memanfaatkan dan memodifikasi basis proyek open-source berlisensi GPL-3.0/MIT termasuk **Mochy Music**, **ViviMusic**, **Metrolist**, **RiMusic**, **SimpMusic**, dan **LRCLIB**. Seluruh hak cipta kode sumber hulu tetap menjadi milik pengembang aslinya sesuai ketentuan GPL-3.0.
