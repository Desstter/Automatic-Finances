# Automatic Finances

App Android que **parsea automáticamente las notificaciones SMS de Bancolombia** para registrar y categorizar transacciones financieras en tiempo real, sin intervención manual. Incluye Machine Learning para categorización inteligente que aprende del comportamiento del usuario.

## Stack

Kotlin · Jetpack Compose · Room Database v4 · MVVM · Navigation Compose · Android Foreground Services

## Features

- **Captura automática 24/7** — servicio en primer plano que procesa todas las notificaciones de Bancolombia (compras, retiros, transferencias)
- **Parser inteligente** — extrae monto, comercio, tipo de transacción y timestamp usando regex sobre el texto del SMS
- **Categorización con ML** — aprende de las correcciones del usuario y alcanza 80%+ de precisión en comercios recurrentes; fallback a reglas por keywords para comercios nuevos
- **Deduplicación** — hashes SHA-256 para evitar registros duplicados
- **Presupuestos mensuales** — límites por categoría con alertas automáticas
- **Metas financieras** — objetivos de ahorro y reducción de gastos
- **Reinicio automático** — el servicio se recupera tras reinicios del dispositivo

## Instalación

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Ejecutar tests
./gradlew test
```

**Requisitos:** Android 8.0+ (API 26), permiso de lectura de notificaciones

## Arquitectura

```
app/
├── service/
│   ├── ForegroundSmsService.kt     # Servicio persistente 24/7
│   ├── SmsNotifListener.kt         # Captura notificaciones
│   └── BootReceiver.kt             # Reinicio automático
├── parser/
│   └── BancolombiaParser.kt        # Regex de SMS de Bancolombia
├── ml/
│   └── CategoryPredictor.kt        # Categorización por ML
├── database/
│   ├── AppDatabase.kt              # Room DB v4 con migraciones
│   └── entities/                   # Transaction, Category, Budget, Goal
└── ui/
    ├── HomeScreen.kt               # Lista de transacciones
    └── TransactionDetailScreen.kt  # Edición y categorización
```

## Pipeline de procesamiento

```
Notificación Bancolombia
    → SmsNotifListener
    → BancolombiaParser (regex)
    → ML Categorizer (80%+ precisión)
    → Deduplicación SHA-256
    → Room Database
    → UI (Jetpack Compose)
```
