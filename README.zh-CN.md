<div align="center">

<img src="docs/icon.svg" width="120" alt="Twitter Hide Ads">

# Twitter Hide Ads

**一个从 X（Twitter）中移除推广帖子和广告的 Xposed 模块。**

[English](README.md) · **简体中文** · [繁體中文](README.zh-TW.md) · [Español](README.es.md) · [Português](README.pt-BR.md) · [Русский](README.ru.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Bahasa Indonesia](README.id.md)

</div>

> [!NOTE]
> 本页是面向用户的简介。完整的技术文档（工作原理、构建、设计）请见 [英文 README](README.md)。

## 功能

- 在 X 绘制之前拦截推广帖子和广告，因此不会留下空白，也不会闪现广告。
- 覆盖首页、帖子详情与回复、搜索以及视频页。
- X 更新后会自动按结构重新查找目标，无需等待模块更新。
- 无法确定时不做任何处理：普通帖子永远不会被误删。

## 要求

| | |
|---|---|
| **Android** | 8.0（API 26）或更高 |
| **应用** | X（`com.twitter.android`） |
| **框架** | [Vector](https://github.com/JingMatrix/Vector) 或其他支持 libxposed API 101+ 的框架，或 [LSPatch](https://github.com/JingMatrix/LSPatch) |
| **Root** | Vector 需要 Root；模块本身不需要 |

## 安装

1. 从 [Releases](https://github.com/MrxSiN/TwitterHideAds/releases) 下载并安装 APK。
2. 在 Vector 中启用 **Twitter Hide Ads**（作用域已固定为 X，无需选择）。
3. 强行停止一次 X，然后重新打开。

**免 Root：** 使用 [LSPatch](https://github.com/JingMatrix/LSPatch) 将本模块嵌入 X（基础 APK 与全部拆分包一起修补）。

## 是否生效

在框架日志中搜索以 `TwitterHideAds:` 开头的行，看到 `enforcement=ACTIVE_ADAPTIVE` 即表示已生效。

## 常见问题

| 问题 | 解决方法 |
|---|---|
| 仍然看到广告 | 确认模块已启用并强行停止过 X。 |
| X 更新后失效 | 请[提交 Issue](https://github.com/MrxSiN/TwitterHideAds/issues)，附上 X 版本号和 `TwitterHideAds:` 日志行。 |

## 免责声明

与 X Corp. 或 Twitter 无关，也未获其认可或赞助。仅供学习和个人使用。X 的更新可能随时导致模块失效。
