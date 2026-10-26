<div align="center">

<img src="docs/icon.svg" width="120" alt="Twitter Hide Ads">

# Twitter Hide Ads

**Um módulo Xposed que remove publicações promovidas e anúncios do X (Twitter).**

[English](README.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [Español](README.es.md) · **Português** · [Русский](README.ru.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Bahasa Indonesia](README.id.md) · [Bahasa Melayu](README.ms.md)

</div>

> [!NOTE]
> Esta página é um resumo para usuários. A documentação técnica completa (funcionamento, build,
> design) está no [README em inglês](README.md).

## O que faz

- Bloqueia publicações promovidas e anúncios antes de o X desenhá-los: sem espaços em branco e sem
  anúncios piscando na tela.
- Funciona na página inicial, nos detalhes das publicações e respostas, na busca e na aba de vídeos.
- Depois de uma atualização do X, reencontra seus alvos pela estrutura, sem esperar uma nova versão
  do módulo.
- Na dúvida, não faz nada: publicações normais nunca são removidas.

## Requisitos

| | |
|---|---|
| **Android** | 8.0 (API 26) ou mais recente |
| **App** | X (`com.twitter.android`) |
| **Framework** | [Vector](https://github.com/JingMatrix/Vector) ou outro framework com libxposed API 101+, ou [LSPatch](https://github.com/JingMatrix/LSPatch) |
| **Root** | Exigido pelo Vector; o módulo em si não precisa |

## Instalação

1. Instale o APK a partir de [Releases](https://github.com/MrxSiN/TwitterHideAds/releases).
2. Ative **Twitter Hide Ads** no Vector (o escopo já é fixo no X).
3. Force a parada do X uma vez e abra-o novamente.

**Sem root:** use o [LSPatch](https://github.com/JingMatrix/LSPatch) para incorporar o módulo ao X
(aplique o patch no APK base junto com todos os splits).

## Como saber se está funcionando

No log do framework, procure linhas que começam com `TwitterHideAds:`.
`enforcement=ACTIVE_ADAPTIVE` significa que está ativo.

## Solução de problemas

| Problema | O que fazer |
|---|---|
| Ainda vejo anúncios | Confira se o módulo está ativado e se você forçou a parada do X. |
| Parou de funcionar após atualizar o X | [Abra uma issue](https://github.com/MrxSiN/TwitterHideAds/issues) com a versão do X e as linhas `TwitterHideAds:` do log. |

## Aviso

Sem afiliação, endosso ou patrocínio da X Corp. ou do Twitter. Para uso educacional e pessoal.
Atualizações do X podem quebrar o módulo sem aviso.
