<div align="center">

<img src="docs/icon.svg" width="120" alt="Twitter Hide Ads">

# Twitter Hide Ads

**一個從 X（Twitter）中移除推廣貼文與廣告的 Xposed 模組。**

[English](README.md) · [简体中文](README.zh-CN.md) · **繁體中文** · [Español](README.es.md) · [Português](README.pt-BR.md) · [Русский](README.ru.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Bahasa Indonesia](README.id.md) · [Bahasa Melayu](README.ms.md)

</div>

> [!NOTE]
> 本頁是給使用者的簡介。完整的技術文件（運作原理、建置、設計）請見 [英文 README](README.md)。

## 功能

- 在 X 繪製之前攔截推廣貼文與廣告，因此不會留下空白，也不會閃現廣告。
- 涵蓋首頁、貼文詳情與回覆、搜尋以及影片頁。
- X 更新後會自動依結構重新尋找目標，無需等待模組更新。
- 無法確定時不做任何處理：一般貼文永遠不會被誤刪。

## 需求

| | |
|---|---|
| **Android** | 8.0（API 26）或更新版本 |
| **應用程式** | X（`com.twitter.android`） |
| **框架** | [Vector](https://github.com/JingMatrix/Vector) 或其他支援 libxposed API 101+ 的框架，或 [LSPatch](https://github.com/JingMatrix/LSPatch) |
| **Root** | Vector 需要 Root；模組本身不需要 |

## 安裝

1. 從 [Releases](https://github.com/MrxSiN/TwitterHideAds/releases) 下載並安裝 APK。
2. 在 Vector 中啟用 **Twitter Hide Ads**（作用範圍已固定為 X，無需選擇）。
3. 強制停止一次 X，然後重新開啟。

**免 Root：** 使用 [LSPatch](https://github.com/JingMatrix/LSPatch) 將本模組嵌入 X（基礎 APK 與所有分割套件一起修補）。

## 是否生效

在框架日誌中搜尋以 `TwitterHideAds:` 開頭的行，看到 `enforcement=ACTIVE_ADAPTIVE` 即表示已生效。

## 常見問題

| 問題 | 解決方法 |
|---|---|
| 仍然看到廣告 | 確認模組已啟用並強制停止過 X。 |
| X 更新後失效 | 請[提交 Issue](https://github.com/MrxSiN/TwitterHideAds/issues)，附上 X 版本號與 `TwitterHideAds:` 日誌行。 |

## 免責聲明

與 X Corp. 或 Twitter 無關，亦未獲其認可或贊助。僅供學習與個人使用。X 的更新可能隨時導致模組失效。
