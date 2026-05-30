# AutomaticFinances

An Android app that reads your bank notifications and builds a fully categorized expense ledger — automatically, in real time, with no manual input.

Deployed at [automatic-finances.moonhellal.com](https://automatic-finances.moonhellal.com) via GitHub Actions CI/CD.

## What it does

Colombian banks push a detailed notification every time money moves. AutomaticFinances intercepts those notifications 24/7, extracts the transaction data, resolves payment gateway codes (PAYU*, MERCPAGO*) to real merchant names, and files everything into a categorized ledger — all before you unlock your phone.

The app also accepts voice input. Say "almuerzo 12, tinto 2" and Gemini parses that into two separate COP transactions, handling Colombian colloquialisms like "tres lucas" (3,000 COP) or "una barra" (100,000 COP) automatically.

## AI integration

**Voice entry via Gemini 2.5 Flash.** Speech-to-text (Colombian Spanish, `es-CO`) feeds a structured prompt to Gemini. The model returns a strict JSON list of transactions — amounts, categories, merchant names — extracted from natural language. Transactions flagged `needsReview=true` surface for confirmation before persisting.

**Merchant resolution.** Payment gateways produce opaque codes. The app ships pre-mapped Colombian merchants and learns from the first user correction — subsequent transactions from the same gateway resolve silently.

**Category suggestion.** Each correction trains a frequency-weighted confidence score per merchant. Once confidence exceeds 0.6, the learned category wins over keyword rules automatically.

## Bank coverage

Primary focus is **Bancolombia** (compras, transferencias, retiros, ingresos). The notification listener also handles Nequi, DaviPlata, BBVA, and Banco de Bogotá.

Deduplication is built in — Android can redeliver the same notification, so every transaction is hashed from its content. The same SMS arriving twice produces the same ID; the second insert is silently discarded and the balance is never double-counted.

ATM withdrawals automatically generate two entries: a bank debit and a cash credit, so cash spending always traces back to a source.

## Architecture

```
Notification (Bancolombia / Nequi / DaviPlata / BBVA / Banco Bogotá)
    ↓
SmsNotifListener          — NotificationListenerService, filters by bank package
    ↓
BancolombiaParser         — pure regex, returns Transaction | null
    ↓
AddTransactionUseCase     — enriches account + category, handles ATM dual-entry
    ↓
Room (INSERT IGNORE)      — content-hash as primary key, deduplication at DB layer
    ↓
Repository Flow           — live stream to ViewModel → Compose UI
    ↓
User correction           — feeds category confidence for next prediction
```

**Stack:** Kotlin · Jetpack Compose · Room · Hilt · Coroutines/Flow · OkHttp · Gemini 2.5 Flash · Material3

## Features

| Feature | Detail |
|---------|--------|
| Auto-capture | Foreground service + BootReceiver — survives reboots, battery optimization |
| Voice entry | Gemini 2.5 Flash parses natural speech into structured transactions |
| Bank coverage | Bancolombia, Nequi, DaviPlata, BBVA, Banco de Bogotá |
| Budgets | Monthly limits per category, real-time % consumed |
| Goals | Savings targets with progress tracking |
| Analytics | Income vs. expense charts, spending trends, category breakdown (Canvas-rendered) |
| Reports | Historical view, date/category/amount filters |
| Theme | Material3, light/dark, dynamic color |
| CI/CD | GitHub Actions → signed APK → VPS Caddy reverse proxy |

## Key files

```
app/src/main/java/com/example/automaticfinances/
├── data/parse/BancolombiaParser.kt           # Notification parsing + dedup hash
├── data/voice/VoiceTransactionParser.kt      # Gemini structured-output parser
├── data/remote/GeminiService.kt             # OkHttp + Gemini 2.5 Flash client
├── data/repo/CategoryRepository.kt          # Keyword rules + learned suggestions
├── data/repo/MerchantResolutionRepository.kt # Gateway-to-merchant mapping
├── domain/AddTransactionUseCase.kt          # Enrichment + idempotent insert
├── domain/BuildVoiceTransactionsUseCase.kt  # Voice draft → Transaction
├── system/SmsNotifListener.kt              # NotificationListenerService
├── data/db/AppDatabase.kt                  # Room, 8 entities, 10 migrations
└── navigation/AppNavigation.kt             # NavHost, slide+fade transitions
```

## Setup

```bash
# Local — add to local.properties
GEMINI_API_KEY=your_key_here

./gradlew assembleDebug       # debug build
./gradlew assembleRelease     # requires signing env vars
./gradlew test                # unit tests
```

**Requirements:** Android 9+ (API 29), notification listener permission, microphone (voice entry)

Release builds are signed via environment variables: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `GEMINI_API_KEY`.
