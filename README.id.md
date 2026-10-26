<div align="center">

<img src="docs/icon.svg" width="120" alt="Twitter Hide Ads">

# Twitter Hide Ads

**Modul Xposed yang menghapus postingan promosi dan iklan dari X (Twitter).**

[English](README.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [Español](README.es.md) · [Português](README.pt-BR.md) · [Русский](README.ru.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · **Bahasa Indonesia** · [Bahasa Melayu](README.ms.md)

</div>

> [!NOTE]
> Halaman ini adalah ringkasan untuk pengguna. Dokumentasi teknis lengkap (cara kerja, build,
> desain) ada di [README bahasa Inggris](README.md).

## Fungsi

- Memblokir postingan promosi dan iklan sebelum X menggambarnya, jadi tidak ada ruang kosong dan
  iklan tidak sempat muncul sekilas.
- Bekerja di Beranda, detail postingan dan balasan, pencarian, serta tab Video.
- Setelah X diperbarui, modul menemukan targetnya kembali berdasarkan struktur, tanpa perlu menunggu
  versi modul yang baru.
- Jika tidak yakin, modul tidak melakukan apa pun: postingan biasa tidak pernah dihapus.

## Persyaratan

| | |
|---|---|
| **Android** | 8.0 (API 26) atau lebih baru |
| **Aplikasi** | X (`com.twitter.android`) |
| **Framework** | [Vector](https://github.com/JingMatrix/Vector) atau framework lain dengan libxposed API 101+, atau [LSPatch](https://github.com/JingMatrix/LSPatch) |
| **Root** | Diperlukan oleh Vector; modulnya sendiri tidak memerlukannya |

## Instalasi

1. Pasang APK dari [Releases](https://github.com/MrxSiN/TwitterHideAds/releases).
2. Aktifkan **Twitter Hide Ads** di Vector (cakupannya sudah ditetapkan ke X).
3. Paksa berhenti X sekali, lalu buka kembali.

**Tanpa root:** gunakan [LSPatch](https://github.com/JingMatrix/LSPatch) untuk menyematkan modul ke
dalam X (patch APK dasar bersama semua split APK).

## Cara memastikan modul aktif

Di log framework, cari baris yang diawali `TwitterHideAds:`.
`enforcement=ACTIVE_ADAPTIVE` berarti modul sudah aktif.

## Pemecahan masalah

| Masalah | Solusi |
|---|---|
| Iklan masih muncul | Pastikan modul aktif dan X sudah dipaksa berhenti. |
| Tidak berfungsi setelah X diperbarui | [Buat issue](https://github.com/MrxSiN/TwitterHideAds/issues) dengan versi X dan baris log `TwitterHideAds:`. |

## Penafian

Tidak berafiliasi dengan, didukung, atau disponsori oleh X Corp. maupun Twitter. Untuk penggunaan
edukasi dan pribadi. Pembaruan X dapat membuat modul berhenti berfungsi tanpa pemberitahuan.
