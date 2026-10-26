<div align="center">

<img src="docs/icon.svg" width="120" alt="Twitter Hide Ads">

# Twitter Hide Ads

**X（Twitter）からプロモーション投稿と広告を取り除く Xposed モジュールです。**

[English](README.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [Español](README.es.md) · [Português](README.pt-BR.md) · [Русский](README.ru.md) · **日本語** · [한국어](README.ko.md) · [Bahasa Indonesia](README.id.md)

</div>

> [!NOTE]
> このページは利用者向けの概要です。技術的な詳細（仕組み、ビルド、設計）は
> [英語の README](README.md) をご覧ください。

## 機能

- X が描画する前にプロモーション投稿と広告をブロックするので、空白も広告のちらつきもありません。
- ホーム、投稿の詳細と返信、検索、動画タブで動作します。
- X のアップデート後も構造から対象を自動で見つけ直すため、モジュールの更新を待つ必要はありません。
- 判断できないときは何もしません。通常の投稿が消されることはありません。

## 動作要件

| | |
|---|---|
| **Android** | 8.0（API 26）以降 |
| **アプリ** | X（`com.twitter.android`） |
| **フレームワーク** | [Vector](https://github.com/JingMatrix/Vector) など libxposed API 101+ 対応のフレームワーク、または [LSPatch](https://github.com/JingMatrix/LSPatch) |
| **Root** | Vector には必要。モジュール自体は不要 |

## インストール

1. [Releases](https://github.com/MrxSiN/TwitterHideAds/releases) から APK をインストールします。
2. Vector で **Twitter Hide Ads** を有効にします（スコープは X に固定済み）。
3. X を一度強制停止してから開きます。

**Root なしの場合：** [LSPatch](https://github.com/JingMatrix/LSPatch) でモジュールを X に組み込みます
（ベース APK とすべての分割 APK をまとめてパッチします）。

## 動作の確認

フレームワークのログで `TwitterHideAds:` で始まる行を探してください。
`enforcement=ACTIVE_ADAPTIVE` と表示されていれば有効です。

## トラブルシューティング

| 症状 | 対処 |
|---|---|
| まだ広告が表示される | モジュールが有効になっているか、X を強制停止したかを確認してください。 |
| X の更新後に動かなくなった | X のバージョンと `TwitterHideAds:` のログ行を添えて [Issue を作成](https://github.com/MrxSiN/TwitterHideAds/issues)してください。 |

## 免責事項

X Corp. および Twitter とは無関係であり、承認や支援も受けていません。学習および個人での利用を
目的としています。X のアップデートにより予告なく動作しなくなる場合があります。
