<div align="center">

<img src="docs/icon.svg" width="120" alt="Twitter Hide Ads">

# Twitter Hide Ads

**Modul Xposed yang membuang hantaran promosi dan iklan daripada X (Twitter).**

[English](README.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [Español](README.es.md) · [Português](README.pt-BR.md) · [Русский](README.ru.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Bahasa Indonesia](README.id.md) · **Bahasa Melayu**

</div>

> [!NOTE]
> Halaman ini ialah ringkasan untuk pengguna. Dokumentasi teknikal penuh (cara ia berfungsi, binaan,
> reka bentuk) terdapat dalam [README bahasa Inggeris](README.md).

## Fungsi

- Menyekat hantaran promosi dan iklan sebelum X memaparkannya, jadi tiada ruang kosong dan iklan
  tidak sempat muncul walaupun sekejap.
- Berfungsi di Laman Utama, butiran hantaran dan balasan, carian, serta tab Video.
- Selepas X dikemas kini, modul ini mencari semula sasarannya berdasarkan struktur, tanpa perlu
  menunggu versi modul yang baharu.
- Jika tidak pasti, modul ini tidak melakukan apa-apa: hantaran biasa tidak akan sesekali dibuang.

## Keperluan

| | |
|---|---|
| **Android** | 8.0 (API 26) atau lebih baharu |
| **Aplikasi** | X (`com.twitter.android`) |
| **Rangka kerja** | [Vector](https://github.com/JingMatrix/Vector) atau rangka kerja lain dengan libxposed API 101+, atau [LSPatch](https://github.com/JingMatrix/LSPatch) |
| **Root** | Diperlukan oleh Vector; modul itu sendiri tidak memerlukannya |

## Pemasangan

1. Pasang APK daripada [Releases](https://github.com/MrxSiN/TwitterHideAds/releases).
2. Aktifkan **Twitter Hide Ads** dalam Vector (skopnya sudah ditetapkan kepada X).
3. Hentikan paksa X sekali, kemudian buka semula.

**Tanpa root:** gunakan [LSPatch](https://github.com/JingMatrix/LSPatch) untuk membenamkan modul ini ke
dalam X (tampal APK asas bersama semua APK split).

## Cara memastikan modul aktif

Dalam log rangka kerja, cari baris yang bermula dengan `TwitterHideAds:`.
`enforcement=ACTIVE_ADAPTIVE` bermaksud modul sudah aktif.

## Penyelesaian masalah

| Masalah | Penyelesaian |
|---|---|
| Iklan masih kelihatan | Pastikan modul diaktifkan dan X sudah dihentikan paksa. |
| Tidak berfungsi selepas X dikemas kini | [Buka isu](https://github.com/MrxSiN/TwitterHideAds/issues) bersama versi X dan baris log `TwitterHideAds:`. |

## Penafian

Tidak bergabung dengan, disokong atau ditaja oleh X Corp. mahupun Twitter. Untuk kegunaan pendidikan
dan peribadi. Kemas kini X boleh menyebabkan modul ini berhenti berfungsi tanpa notis.
