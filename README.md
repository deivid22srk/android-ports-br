# Android Ports BR

Feed de notícias em tela cheia, com swipe vertical estilo TikTok/Kwai, para a comunidade brasileira de ports, custom ROMs, modding, apps e lançamentos. Visual **Nothing OS × Apple**: preto OLED, um único vermelho (`#D71921`), tipografia dot-matrix pontual, vidro translúcido e molas.

> **Conteúdo de demonstração:** `server/data/seed.json` e `app/src/main/assets/seed_news.json` trazem 32 notícias escritas para o projeto. Os links apontam para as fontes, sem republicar artigos completos. O servidor também agrega feeds RSS reais configurados em `server/sources.json`.

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Stack e arquitetura](#stack-e-arquitetura)
- [Design system](#design-system)
- [Como rodar o servidor](#como-rodar-o-servidor)
- [Conectar o app ao servidor](#conectar-o-app-ao-servidor)
- [Offline: servidor → cache → seed](#offline-servidor--cache--seed)
- [Build do APK](#build-do-apk)
- [CI / GitHub Actions](#ci--github-actions)
- [Performance](#performance)
- [Estrutura](#estrutura)
- [Decisões técnicas](#decisões-técnicas)

---

## Funcionalidades

- **Feed full-screen** com `VerticalPager`, snap preciso, paginação por cursor e pré-carregamento da próxima página/imagem.
- **Duplo toque para curtir** com coração animado, haptic e persistência em Room.
- **Salvar, compartilhar e abrir original**; leitor em bottom sheet arrastável com blur/glass e conteúdo completo.
- **Curtidas e salvos offline**, sempre disponíveis na aba Salvos.
- **Explorar** com busca (debounce de 350 ms), grade de categorias e “Em alta”.
- **Ajustes** com tema escuro/claro/sistema, haptics, reduzir animações, URL do servidor, teste de conexão e limpeza de cache.
- **4 abas** em bottom navigation flutuante; estado e rolagem preservados entre elas.
- **Automático offline**: servidor → cache Room → seed embutido no APK, com indicador “MODO OFFLINE”.

## Stack e arquitetura

| Camada | Escolha |
|---|---|
| Linguagem/UI | Kotlin 2.1.10 + Jetpack Compose (Material 3 apenas como base) |
| SDK | `minSdk 26`, `targetSdk 35`, `compileSdk 35` |
| Arquitetura | MVVM + UDF, `UiState` imutável + `StateFlow`, camadas `data/domain/ui` |
| Rede | Retrofit 2.11 + OkHttp + `kotlinx.serialization` |
| Imagens | Coil 3 (`coil-compose` + `coil-network-okhttp`), cache de memória/disco e downsampling |
| Persistência | Room 2.7.1 (notícias, curtidas, salvos, cache) + DataStore Preferences |
| DI | Container manual (`di/AppContainer.kt`), sem Hilt/Dagger |
| Navegação | Navigation Compose com rotas tipadas (`@Serializable`) |
| Build | Kotlin DSL, Version Catalog, Gradle 8.9, AGP 8.7.3, R8 full mode |
| Animações | `spring`, `graphicsLayer`, `Animatable`; leitura de pager na fase de desenho |
| Backend | Node.js 20+ + Fastify, sem banco externo |

Fluxo do feed:

```
FeedViewModel --(intenção)--> NewsRepository
                                  ├── Retrofit (servidor local / RSS agregado)
                                  ├── Room (cache + curtidas/salvos)
                                  └── assets/seed_news.json (fallback absoluto)
```

## Design system

Todos os tokens ficam em `ui/theme/Theme.kt` (nada de cor/tamanho solto nas telas):

- **Cores:** OLED `#000000`, superfícies `#0A0A0A`/`#151515`, texto `#FFFFFF`/`#A1A1A6`, acento único `#D71921`, hairline `#2EFFFFFF`. Tema claro equivalente.
- **Tipografia:** Doto (dot-matrix, variável) em logo, títulos de seção, labels e números; Inter no corpo. Arquivos em `res/font/`.
- **Formas:** squircle-like com raios 14–36 dp; pílulas para chips e navigation bar.
- **Espaçamento e movimento:** `Spacing` e `Motion` centralizados, com `LocalReduceMotion`.
- **Haptics:** `LocalHapticsEnabled`, respeitando Ajustes.
- **Glass:** `GlassSurface` com gradiente translúcido + borda de 0,7 dp; blur real (`Modifier.blur`) aplicado nas camadas decorativas em API 31+ e translucidez como fallback.
- Contraste AA, alvos de toque ≥ 48 dp e textos sobre imagem sempre com scrim inferior.

## Como rodar o servidor

Requisitos: Node.js 20+ (testado em Node 20.19 / Termux).

```bash
cd server
npm install
npm start
# http://localhost:3000/api/v1/health
```

Porta configurável:

```bash
PORT=8080 npm start
```

No Termux:

```bash
pkg install nodejs
cd server && npm install && npm start
```

### Endpoints

Prefixo `/api/v1`:

| Método | Rota | Descrição |
|---|---|---|
| GET | `/health` | status, modo (`live`/`seed`/`cache`), contagem e última atualização |
| GET | `/news?category=&cursor=&limit=` | paginação por cursor, mais recentes primeiro |
| GET | `/news/:id` | detalhe da notícia |
| GET | `/categories` | categorias com contagem |
| GET | `/search?q=` | busca em título, resumo e conteúdo |

Exemplos:

```bash
curl -s localhost:3000/api/v1/health | jq
curl -s "localhost:3000/api/v1/news?limit=3" | jq
curl -s "localhost:3000/api/v1/news?category=ports" | jq
curl -s "localhost:3000/api/v1/news/seed-001" | jq
curl -s "localhost:3000/api/v1/search?q=rom" | jq
curl -s "localhost:3000/api/v1/categories" | jq
```

Respostas usam **gzip**, **ETag** (com `304 Not Modified`) e `Cache-Control`. O refresh de RSS roda em memória a cada **15 minutos**, sem nunca bloquear uma requisição; o resultado vira `server/data/cache.json` (ignorado pelo Git). Se todas as fontes falharem, o seed continua respondendo.

## Conectar o app ao servidor

| Cenário | URL em Ajustes | Comando |
|---|---|---|
| Emulador Android | `http://10.0.2.2:3000` (padrão) | — |
| Aparelho via USB | `http://127.0.0.1:3000` | `adb reverse tcp:3000 tcp:3000` |
| Termux no mesmo aparelho | `http://127.0.0.1:3000` | — |

HTTP em texto claro é liberado **somente** para `localhost`, `127.0.0.1` e `10.0.2.2` em `res/xml/network_security_config.xml`. Todo o resto exige HTTPS.

## Offline: servidor → cache → seed

1. **Servidor**: tentativa de rede (timeout curto de 5–12 s, requisição cancelável).
2. **Cache Room**: exibido imediatamente na abertura, com curtidas/salvos preservados.
3. **Seed embutido**: `app/src/main/assets/seed_news.json`, carregado se não houver banco/rede.

O APK funciona 100% sem servidor: o seed é gravado no Room na primeira execução e o feed continua com paginação local. O selo **MODO OFFLINE** aparece no topo quando a última tentativa de rede falhou.

## Build do APK

Pré-requisitos locais: JDK 17 + Android SDK 35.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Release assinado localmente (opcional):

```bash
export CI_KEYSTORE_FILE=/caminho/keystore.jks
export CI_KEYSTORE_PASSWORD=...
export CI_KEY_ALIAS=androidports
export CI_KEY_PASSWORD=...
./gradlew assembleRelease
```

Sem essas variáveis, o Gradle gera um release **não assinado** — útil para validar R8/shrink. O workflow sempre assina com um keystore efêmero de CI ou com os secrets abaixo.

Configuração de release:

- R8 em **full mode** + `shrinkResources`.
- ABIs restritas a `arm64-v8a` e `armeabi-v7a`.
- Recursos limitados a `pt`/`en`.
- `baseline-prof.txt` embutido em `app/src/main/`.
- Ícones vetoriais e fontes embutidas.

## CI / GitHub Actions

`.github/workflows/build.yml` — workflow **“Build APK”**:

- Dispara em `push` na `main`, `pull_request`, tags `v*` e `workflow_dispatch`.
- `permissions: contents: write`.
- Java 17 Temurin, Gradle cache e Android SDK 35.
- Gera keystore de CI com `keytool` (ou usa os secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, se existirem).
- Executa `./gradlew assembleRelease --no-daemon --stacktrace`.
- Renomeia para `Android-Ports-BR-<versão>.apk` e publica o artefato `android-ports-br-apk`.
- Em tags `v*`, cria um GitHub Release com o APK.

> Nenhuma chave ou senha real é versionada. O keystore de CI é descartável.

## Performance

Metas e o que já está implementado:

- 60 fps (e fluidez em 90/120 Hz) com `VerticalPager` + `beyondViewportPageCount = 1`.
- Modelos `@Immutable`, `key` estável por id, lambdas estáveis, `remember`/`rememberUpdatedState`.
- Progresso do pager e offsets lidos em `drawBehind`/`graphicsLayer`, sem recomposição por frame.
- Coil com downsampling por tamanho de exibição, crossfade curto, cache de disco (192 MB) e HTTP cache OkHttp (8 MB); pré-carga da próxima imagem.
- Nada de rede na main thread; `collectAsStateWithLifecycle`; nenhum serviço em background.
- SplashScreen API + `AppContainer`/banco/Coil inicializados de forma preguiçosa; `StrictMode` apenas no debug.
- Release com R8 full + shrink; ícones vetoriais.

### Medições

| Métrica | Resultado |
|---|---|
| APK release assinado | **preencher após o primeiro build verde do CI** |
| Tempo do workflow | **preencher após o primeiro build verde do CI** |
| `dumpsys gfxinfo` (frames lentos) | Não medido neste ambiente (sem dispositivo/emulador Android). |
| Tempo de inicialização | Não medido neste ambiente. |

Este ambiente de desenvolvimento é um Debian/Termux aarch64 sem Android SDK/emulador; a compilação e a assinatura são feitas pelo GitHub Actions. Para medir em um aparelho:

```bash
adb shell am start -W -n br.androidports/.MainActivity
adb shell dumpsys gfxinfo br.androidports reset
# role o feed por ~10 s
adb shell dumpsys gfxinfo br.androidports
```

Alvos: < 1% de frames lentos, startup < 800 ms em aparelho intermediário, APK release ~10 MB.

## Estrutura

```
android-ports-br/
├── app/
│   ├── src/main/java/br/androidports/
│   │   ├── data/          # api (Retrofit/DTOs), local (Room/DataStore), repository, seed
│   │   ├── domain/        # NewsItem, categorias, FeedFilter
│   │   ├── di/            # AppContainer manual
│   │   └── ui/            # theme, components, feed, explore, saved, settings, reader, nav
│   ├── src/main/assets/seed_news.json
│   ├── src/main/res/      # fontes Doto/Inter, drawables vetoriais, xml
│   └── src/main/baseline-prof.txt
├── server/
│   ├── src/               # index.js (Fastify), store.js, rss.js, refresh.js
│   ├── data/seed.json     # 32 notícias PT-BR de demonstração
│   ├── sources.json       # feeds RSS configuráveis
│   └── package.json
├── .github/workflows/build.yml
├── gradle/ + gradlew + gradlew.bat
└── README.md
```

## Decisões técnicas

- **Fastify em vez de Express:** uma dependência, HTTP/2-ready e baixo overhead; gzip/ETag implementados com `node:zlib`/`node:crypto` para não adicionar plugins.
- **Retrofit em vez de Ktor:** build menor e mais rápido com OkHttp, que já é usado pelo Coil.
- **DI manual:** menos classes geradas, startup mais rápido e build mais leve que Hilt/Dagger.
- **DataStore em vez de SharedPreferences:** leitura assíncrona e reativa para tema/servidor.
- **Room:** persistência transacional de salvos/curtidas e cache; seed é importado apenas quando o banco está vazio.
- **Sem biblioteca de blur externa:** `Modifier.blur` + camadas translúcidas mantêm o APK leve e funcionam de API 26 a 35.
- **Seed com 32 itens:** garante feed completo no primeiro uso e em qualquer cenário offline.
- **Categorias heurísticas no RSS:** o agregador classifica por palavras-chave (port, ROM, app, tutorial), mantendo a categoria padrão da fonte como fallback.

## Licenças de fontes

- **Doto** — Copyright 2024 The Doto Project Authors, SIL Open Font License 1.1.
- **Inter** — Copyright The Inter Project Authors, SIL Open Font License 1.1.

As licenças completas estão em `licenses/`.

---

Feito para a comunidade Android BR. Pull requests e novas fontes RSS são bem-vindos.
