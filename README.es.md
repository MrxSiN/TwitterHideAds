<div align="center">

<img src="docs/icon.svg" width="120" alt="Twitter Hide Ads">

# Twitter Hide Ads

**Un módulo de Xposed que elimina las publicaciones promocionadas y los anuncios de X (Twitter).**

[English](README.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · **Español** · [Português](README.pt-BR.md) · [Русский](README.ru.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Bahasa Indonesia](README.id.md)

</div>

> [!NOTE]
> Esta página es un resumen para usuarios. La documentación técnica completa (funcionamiento,
> compilación, diseño) está en el [README en inglés](README.md).

## Qué hace

- Bloquea las publicaciones promocionadas y los anuncios antes de que X los dibuje: sin huecos en
  blanco y sin destellos de anuncios.
- Funciona en Inicio, en el detalle de las publicaciones y sus respuestas, en la búsqueda y en la
  pestaña de vídeos.
- Tras una actualización de X vuelve a encontrar sus objetivos por estructura, sin esperar a una
  nueva versión del módulo.
- Si no está seguro, no hace nada: las publicaciones normales nunca se eliminan.

## Requisitos

| | |
|---|---|
| **Android** | 8.0 (API 26) o superior |
| **App** | X (`com.twitter.android`) |
| **Framework** | [Vector](https://github.com/JingMatrix/Vector) u otro framework con libxposed API 101+, o [LSPatch](https://github.com/JingMatrix/LSPatch) |
| **Root** | Lo requiere Vector; el módulo en sí no lo necesita |

## Instalación

1. Instala el APK desde [Releases](https://github.com/MrxSiN/TwitterHideAds/releases).
2. Activa **Twitter Hide Ads** en Vector (el ámbito ya está fijado en X).
3. Fuerza la detención de X una vez y vuelve a abrirlo.

**Sin root:** usa [LSPatch](https://github.com/JingMatrix/LSPatch) para integrar el módulo en X
(parchea el APK base junto con todos los splits).

## Cómo comprobar que funciona

En el registro del framework, busca las líneas que empiezan por `TwitterHideAds:`.
`enforcement=ACTIVE_ADAPTIVE` significa que está activo.

## Solución de problemas

| Problema | Qué hacer |
|---|---|
| Sigo viendo anuncios | Comprueba que el módulo está activado y que forzaste la detención de X. |
| Dejó de funcionar tras actualizar X | [Abre un issue](https://github.com/MrxSiN/TwitterHideAds/issues) con la versión de X y las líneas `TwitterHideAds:` del registro. |

## Aviso

Sin afiliación, respaldo ni patrocinio de X Corp. ni de Twitter. Uso educativo y personal. Las
actualizaciones de X pueden romper el módulo sin previo aviso.
