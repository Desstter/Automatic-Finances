# Auditoría integral — AutomaticFinances (2026-06-01)

Evaluación multidisciplinar (producto, UX/UI, ingeniería, arquitectura, seguridad) del estado
actual tras las Fases 1–6 de saneamiento. Honesta y priorizada. El núcleo ya está sano; lo que
queda es mayormente endurecimiento, confianza del usuario y ambición de producto.

---

## 1. Veredicto general

**Es un proyecto serio, no un juguete.** El motor financiero está, hoy, mejor diseñado que el de
muchas apps comerciales: idempotencia real, atomicidad por transacción de DB, parser puro y
testeable, balance como caché materializada de una fuente derivada. La arquitectura es limpia y
consistente (UI → ViewModel → Repository → DAO → Room, Hilt, Flow, IO). 52 tests unitarios verdes.

Lo que separa esto de "excepcional para uso diario" no es el núcleo, son tres frentes:
1. **Confianza** — hoy una captura errónea altera el saldo en silencio, sin revisión.
2. **Endurecimiento** — datos bancarios en claro y respaldados a la nube; release sin R8.
3. **Ambición de producto** — captura datos muy bien, pero todavía *informa* poco y *actúa* menos.

---

## 2. Fortalezas (mantener y proteger)

- **Invariante financiero sólido.** `AddTransactionUseCase` enriquece → inserta con `INSERT IGNORE`
  → recomputa balance, todo dentro de `transactionRunner.runInTransaction {}`. El RETIRO de doble
  entrada no puede quedar a medias. Balance = derivado (apertura + movimientos), no delta incremental.
- **Parser desacoplado y determinista.** Cero dependencias de DB/DI; `tryParse(text, postTime)`
  con id estable SHA-256 sobre minuto+monto+tipo+last4+merchant+preview → dedup vía PK.
- **Filtrado de notificaciones correcto.** Match por paquete exacto (`BANK_APPS`), y solo apps de
  mensajería exigen keyword bancaria. Ya no hay sobre-match por "android".
- **Listener como fuente de verdad.** Se eliminó el FGS vestigial; el scope revive en reconexión.
- **Disciplina de código.** Sin `runBlocking`/`GlobalScope` en producción, 1 solo TODO, tokens de
  spacing/motion, sin emojis en chrome, montos siempre en `Long` centavos.
- **Higiene de secretos parcial.** Keystore y `local.properties` en `.gitignore`; API key Gemini por
  header, no por query param; sin PII en logcat.

---

## 3. Hallazgos por área

Severidad: 🔴 alta · 🟠 media · 🟡 baja

### 3.1 Seguridad y privacidad

- 🔴 **SEG-1 · Datos bancarios en claro + respaldados a la nube.** `autobook.db` es SQLite sin cifrar
  con todo el historial financiero y last4. `AndroidManifest`: `allowBackup="true"` y los XML de
  backup están **vacíos** (solo comentarios de plantilla) → Auto Backup sube la base completa a Google
  Drive y `adb backup` la extrae sin root. Para una app de finanzas es la exposición más grave.
  **Acción:** decidir cifrado en reposo (SQLCipher / `androidx.security`), y como mínimo inmediato
  excluir la DB del backup o poner `allowBackup="false"`.
- 🟠 **SEG-2 · Release sin R8/ofuscación.** `isMinifyEnabled=false`, `isShrinkResources=false`. APK
  más grande y código de una app financiera sin ofuscar. Activar minify+shrink (probar el parser y
  Room/Hilt con reglas Proguard, ya hay `proguard-rules.pro`).
- 🟠 **SEG-3 · API key de Gemini embebida en el APK.** `BuildConfig.GEMINI_API_KEY` viaja en el binario;
  decompilar = extraer la clave. Inherente a llamadas cliente. Fix real: proxy propio (tu VPS Caddy ya
  existe) que firme las peticiones; el cliente nunca ve la clave. Mientras tanto, restringir la key.
- 🟡 **SEG-4 · Password de keystore en VCS.** El `.keystore` no está versionado (bien), pero la
  contraseña fallback `"123456"` está en `build.gradle.kts`. Moverla a `local.properties`/env.

### 3.2 Correctitud financiera y parser

- 🔴 **FIN-1 · Inserción silenciosa sin revisión = riesgo de confianza.** No hay paso de confirmación
  para transacciones auto-capturadas. Un falso positivo (ver FIN-2) altera el saldo sin que el usuario
  lo note, y el modelo aprende de un dato sucio. **Esta es la mejor oportunidad producto+correctitud:**
  convertir el riesgo en feature → cola de "Por revisar" para capturas de baja confianza (ver PROD-1).
- 🟠 **FIN-2 · Fallback genérico propenso a falsos positivos.** `genericCompraRegex` (`Compra… 1234`)
  + keyword-match en apps de mensajería puede fabricar transacciones desde un SMS no bancario que
  contenga "compra/pago" y 4 dígitos. Marcar lo capturado por `notif:generic` con baja confianza y
  enrutarlo a revisión en vez de insertarlo directo.
- 🟠 **FIN-3 · Cobertura de bancos hardcodeada.** Solo Bancolombia/Nequi/DaviPlata (+ regex sueltos).
  Añadir un banco o un formato nuevo de SMS exige tocar código y publicar. No escala. (Ver PROD-4:
  parser entrenable por IA.)
- 🟠 **FIN-4 · Sesgo retroactivo de saldo de apertura.** Instalaciones existentes con apertura
  auto-sembrada (efectiva = hoy, inclusiva del día) mantienen el sesgo hasta reconfigurar. Falta una
  migración de datos única. `OpeningBalanceRepository.recalculateAccountBalances()` aún puede
  doble-contar transacciones del mismo día si la apertura venía de un saldo ya inclusivo.
- 🟡 **FIN-5 · Moneda y zona horaria fijas.** COP y `America/Bogota` hardcodeados en el parser. OK para
  uso personal hoy; bloquea cualquier viaje/compra en otra divisa.

### 3.3 Arquitectura y mantenibilidad

- 🟠 **MANT-1 · `MerchantResolution` 100% muerto pero persistido en el esquema.** Entidad+DAO+repo+DI +
  ~30 entradas, `MIGRATION_10_11` crea la tabla. Decidir antes del próximo release: terminar de cablear
  (lo usa `AddTransactionUseCase.enrich()`… pero el repo no se llena nunca) o eliminar con migración.
- 🟠 **MANT-2 · Reglas de categorías hardcodeadas en Kotlin.** `CategoryRepository` es un `when` gigante
  con conocimiento de comercios colombianos incrustado en código. Debería ser una tabla de datos
  editable por el usuario. Además `getIntelligentCategorySuggestion()` solo usa keywords de gasto,
  ignora ingresos.
- 🟡 **MANT-3 · Código muerto aislado restante.** `AccountDao.getCurrentBalanceHistory` +
  `BalanceHistory`/`BalanceChangeType` en `Account.kt` (sin uso; toca DAO+fake de test).
- 🟡 **MANT-4 · Archivos grandes.** `BancolombiaParser` 679, `AnalyticsRepository` 614, varias pantallas
  500+. Manejable, pero el parser pide extraer las regex a un catálogo declarativo (ver FIN-3/PROD-4).

### 3.4 Rendimiento y escalabilidad

- 🟡 **PERF-1 · Analítica recalcula categoría por categoría.** `getCategorySpendingForMonth` itera
  todas las categorías y lanza una query por cada una (N+1). Con pocas categorías es trivial; con
  histórico largo conviene una sola query `GROUP BY categoryId`.
- 🟡 **PERF-2 · Gráficas en Canvas a mano.** Decisión deliberada (sin librería 3rd-party). Bien para
  control, pero es superficie de mantenimiento y de bugs de layout; vigilar al crecer.
- 🟢 Home ya filtra en memoria sobre un único colector y usa `refreshBalances()` ligero. Bien.

### 3.5 UX/UI y producto

- 🟢 **IA limpia.** 4 raíces (Inicio, Movimientos, Análisis, Ajustes), back stack predecible, Home como
  dashboard, Movimientos como única superficie de filtrado. Onboarding ya existe.
- 🟠 **UX-1 · La app captura genial pero "habla" poco.** No hay digest semanal/mensual, ni alertas de
  anomalía, ni proyección de fin de mes. El valor diario de una app de finanzas está en lo que te
  *dice*, no solo en lo que registra.
- 🟠 **UX-2 · Sin entrada rápida fuera de la app.** No hay widget de pantalla de inicio ni quick tile.
  Para uso diario, ver saldo + gasto de hoy sin abrir la app es altísimo valor/bajo esfuerzo.
- 🟡 **UX-3 · Permiso de batería quizá innecesario.** `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` y el
  `BootReceiver` tienen valor dudoso ahora que el listener se re-vincula solo (`requestRebind`).
  Verificar si se pueden quitar para reducir fricción de permisos.

### 3.6 Testing y observabilidad

- 🟠 **TEST-1 · Sin tests de instrumentación/UI ni de migraciones reales.** `exportSchema=true` ya
  habilita `MigrationTestHelper` de aquí en adelante, pero falta el primer test de migración y
  cualquier test de los flujos nuevos de Home/Movimientos en dispositivo.
- 🟠 **OBS-1 · Fallos del parser se tragan en silencio.** `handleParserError`/`handleDatabaseError` solo
  hacen `Log.w`. No tienes visibilidad de qué SMS bancarios no se están capturando. Sin crash/analytics
  reporting (aceptable en personal, pero los SMS no parseados son justo la señal que necesitas para
  mejorar el parser — ver PROD-4).

---

## 4. Ideas ambiciosas (efecto "wow") + automatizaciones

Ordenadas de "convierte un riesgo en feature" a "moonshot".

- **PROD-1 · Cola "Por revisar" (revisión de baja confianza).** Capturas de `notif:generic` o
  categoría dudosa entran como `pendingReview` y NO afectan el saldo confirmado hasta un tap del
  usuario (confirmar / corregir categoría / descartar). Resuelve FIN-1 y FIN-2 y alimenta el ML con
  datos limpios. **Máximo impacto, esfuerzo medio.**
- **PROD-2 · Notificación de captura con chips de categoría.** Cuando se registra una transacción, la
  notificación de resultado ofrece 2-3 categorías sugeridas como acciones; un tap confirma o corrige
  → feed directo a `UserCategoryPreference`. Cierra el loop de aprendizaje sin abrir la app.
- **PROD-3 · Widget + Quick Settings tile.** Widget: saldo + gasto de hoy + acceso a voz. Tile: entrada
  por voz en un tap. Alto valor diario, esfuerzo bajo-medio.
- **PROD-4 · Parser entrenable por IA (moonshot, resuelve FIN-3).** Los SMS bancarios no parseados se
  guardan; con un tap el usuario los envía a Gemini, que extrae monto/comercio/fecha y *propone una
  plantilla/regex* para ese formato. El usuario confirma → la cobertura crece sin publicar versión.
  Esto convierte "solo Bancolombia" en "cualquier banco que te escriba".
- **PROD-5 · Detección de suscripciones recurrentes.** Detectar cargos mensuales repetidos (Netflix,
  Spotify, gimnasio) → "$X/mes en suscripciones", avisar de subidas de precio y de suscripciones
  olvidadas. Es el insight que más engancha en apps de finanzas.
- **PROD-6 · Proyección de flujo y alertas de anomalía.** Run-rate → "a este ritmo cerrarás el mes en
  $X / te pasarás del presupuesto el día 22". Alertas: cargo duplicado, transacción inusualmente alta,
  "gastaste 40% más en restaurantes que el mes pasado".
- **PROD-7 · Consulta en lenguaje natural sobre datos locales.** "¿cuánto gasté en comida este mes?"
  vía Gemini con function-calling sobre tus queries de Room (los datos no salen, solo la pregunta y un
  esquema). Reaprovecha toda la capa de voz que ya tienes.
- **PROD-8 · Export/backup cifrado + Google Sheets.** Export CSV/Sheets para análisis libre, y backup
  cifrado restaurable (resuelve también la dependencia del Auto Backup de SEG-1 con algo bajo tu control).
- **PROD-9 · Digest semanal/mensual.** Notificación programada (ya sabes hacer notificaciones y tienes
  CI/servidor) con el resumen y 1-2 insights accionables.

---

## 5. Roadmap priorizado

| # | Ítem | Impacto | Esfuerzo | Por qué ahora |
|---|------|---------|----------|----------------|
| 1 | SEG-1 excluir DB del backup / `allowBackup=false` | Alto | **Muy bajo** | Fuga de datos bancarios, fix de minutos |
| 2 | FIN-1/FIN-2 + PROD-1 cola "Por revisar" | Alto | Medio | Protege el saldo y la confianza; base del ML |
| 3 | SEG-2 activar R8/minify en release | Medio | Bajo | Higiene de release, gratis |
| 4 | PROD-3 widget + quick tile | Alto | Bajo-Medio | Valor diario, diferenciador |
| 5 | PROD-2 chips de categoría en notificación | Alto | Medio | Cierra loop de aprendizaje |
| 6 | SEG-1 cifrado en reposo (SQLCipher) | Alto | Medio-Alto | Lo correcto para datos bancarios |
| 7 | PROD-5 suscripciones + PROD-6 anomalías | Alto | Medio | El "wow" de insights |
| 8 | MANT-1 decidir MerchantResolution (cablear/eliminar) | Medio | Bajo | Deuda en el esquema antes del release |
| 9 | FIN-4 migración del sesgo de apertura | Medio | Medio | Corrige saldos de instalaciones viejas |
| 10 | PROD-4 parser entrenable por IA | Muy alto | Alto | Moonshot: rompe el techo de "1 banco" |
| 11 | TEST-1 primer test de migración + UI flows | Medio | Medio | Blindar antes de crecer |
| 12 | MANT-2 reglas de categoría a datos editables | Medio | Medio | Mantenibilidad + personalización |

**Simplificar / eliminar:** MerchantResolution muerto (MANT-1), código muerto de balance history
(MANT-3), permiso de batería + BootReceiver si se confirman innecesarios (UX-3).

---

## 6. Estado de fases previas (contexto)

Fases 1–6 completadas y verificadas (build + 52 tests): idempotencia, atomicidad (`TransactionRunner`),
parser puro, balance materializado=derivado, FGS eliminado, bug de parseo de montos arreglado, IA de
navegación rediseñada, schema export habilitado. Detalle en memoria `audit-backlog`.
