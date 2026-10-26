<div align="center">

<img src="docs/icon.svg" width="120" alt="Twitter Hide Ads">

# Twitter Hide Ads

**X(Twitter)에서 프로모션 게시물과 광고를 없애 주는 Xposed 모듈입니다.**

[English](README.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [Español](README.es.md) · [Português](README.pt-BR.md) · [Русский](README.ru.md) · [日本語](README.ja.md) · **한국어** · [Bahasa Indonesia](README.id.md)

</div>

> [!NOTE]
> 이 페이지는 사용자를 위한 요약입니다. 전체 기술 문서(동작 방식, 빌드, 설계)는
> [영어 README](README.md)를 참고하세요.

## 기능

- X가 화면에 그리기 전에 프로모션 게시물과 광고를 차단하므로 빈 공간이나 광고 깜빡임이 없습니다.
- 홈, 게시물 상세와 답글, 검색, 동영상 탭에서 동작합니다.
- X가 업데이트되어도 구조를 기준으로 대상을 다시 찾으므로 모듈 업데이트를 기다릴 필요가 없습니다.
- 확실하지 않으면 아무것도 하지 않습니다. 일반 게시물은 절대 지워지지 않습니다.

## 요구 사항

| | |
|---|---|
| **Android** | 8.0(API 26) 이상 |
| **앱** | X(`com.twitter.android`) |
| **프레임워크** | [Vector](https://github.com/JingMatrix/Vector) 또는 libxposed API 101+를 지원하는 다른 프레임워크, 또는 [LSPatch](https://github.com/JingMatrix/LSPatch) |
| **루트** | Vector에 필요하며, 모듈 자체에는 필요 없습니다 |

## 설치

1. [Releases](https://github.com/MrxSiN/TwitterHideAds/releases)에서 APK를 설치합니다.
2. Vector에서 **Twitter Hide Ads**를 활성화합니다(범위는 X로 고정되어 있습니다).
3. X를 한 번 강제 종료한 뒤 다시 엽니다.

**루트 없이:** [LSPatch](https://github.com/JingMatrix/LSPatch)로 모듈을 X에 내장합니다
(기본 APK와 모든 분할 APK를 함께 패치합니다).

## 동작 확인

프레임워크 로그에서 `TwitterHideAds:`로 시작하는 줄을 찾으세요.
`enforcement=ACTIVE_ADAPTIVE`가 보이면 활성화된 것입니다.

## 문제 해결

| 문제 | 해결 방법 |
|---|---|
| 아직 광고가 보입니다 | 모듈이 활성화되어 있고 X를 강제 종료했는지 확인하세요. |
| X 업데이트 후 동작하지 않습니다 | X 버전과 `TwitterHideAds:` 로그 줄을 첨부해 [이슈를 등록](https://github.com/MrxSiN/TwitterHideAds/issues)해 주세요. |

## 면책 조항

X Corp. 또는 Twitter와 관련이 없으며, 그들의 승인이나 후원을 받지 않았습니다. 학습 및 개인적인
용도로 제공됩니다. X 업데이트로 인해 예고 없이 동작하지 않을 수 있습니다.
