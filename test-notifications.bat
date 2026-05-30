@echo off
REM Script de testing para notificaciones de AutomaticFinances (Windows)
REM Uso: test-notifications.bat [tipo]

setlocal enabledelayedexpansion

REM Verificar que ADB está disponible
where adb >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: ADB no esta instalado o no esta en el PATH
    exit /b 1
)

REM Verificar que hay un dispositivo conectado
adb devices | findstr /C:"device" >nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: No hay dispositivos conectados. Conecta un dispositivo o emulador.
    exit /b 1
)

echo Dispositivo detectado
echo.

set TYPE=%1

if "%TYPE%"=="" (
    goto :usage
)

if "%TYPE%"=="compra-cop" goto :compra-cop
if "%TYPE%"=="compra-usd" goto :compra-usd
if "%TYPE%"=="transferencia" goto :transferencia
if "%TYPE%"=="retiro" goto :retiro
if "%TYPE%"=="ingreso" goto :ingreso
if "%TYPE%"=="nomina" goto :nomina
if "%TYPE%"=="gmail-cop" goto :gmail-cop
if "%TYPE%"=="gmail-usd" goto :gmail-usd
if "%TYPE%"=="all" goto :all

goto :usage

:compra-cop
echo === TEST: COMPRA EN PESOS (COP) ===
adb emu sms send +573001234567 "Bancolombia: Compraste COP50.000,00 en RAPPI con tu T.Cred *2670, el 20/12/2025 a las 14:30. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca."
echo Enviado: Compra Rappi COP50.000
echo.
goto :end

:compra-usd
echo === TEST: COMPRA EN DOLARES (USD) ===
adb emu sms send +573001234567 "Bancolombia: Compraste USD25,50 en AMAZON WEB SERVICES con tu T.Cred *2670, el 20/12/2025 a las 16:45. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca."
echo Enviado: Compra AWS USD25.50
echo.
goto :end

:transferencia
echo === TEST: TRANSFERENCIA ===
adb emu sms send +573001234567 "Bancolombia: Transferiste COP100.000,00 desde tu cuenta *2670 a la cuenta *5678 el 20/12/2025 a las 10:15. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca."
echo Enviado: Transferencia COP100.000
echo.
goto :end

:retiro
echo === TEST: RETIRO CAJERO ===
adb emu sms send +573001234567 "Bancolombia: Retiraste COP200.000,00 en CAJERO_EXITO_80 de tu T.Deb *2670 el 20/12/2025 a las 18:20. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca."
echo Enviado: Retiro cajero COP200.000
echo.
goto :end

:ingreso
echo === TEST: INGRESO/TRANSFERENCIA RECIBIDA ===
adb emu sms send +573001234567 "Bancolombia: Recibiste COP150.000,00 a tu cuenta *2670 el 20/12/2025 a las 09:00. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca."
echo Enviado: Ingreso recibido COP150.000
echo.
goto :end

:nomina
echo === TEST: PAGO DE NOMINA ===
adb emu sms send +573001234567 "Bancolombia: Recibiste un pago de Nomina de EMPRESA TECH SAS por COP3.500.000,00 en tu cuenta de Ahorros *2670 el 01/12/2025 a las 08:00. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca."
echo Enviado: Nomina COP3.500.000
echo.
goto :end

:gmail-cop
echo === TEST: NOTIFICACION GMAIL - COMPRA COP ===
adb shell cmd notification post -t "Bancolombia" "bancolombia_gmail_001" "Bancolombia: Compraste COP300,00 en DLO*DiDi CO Payin (R con tu T.Cred *2670, el 20/12/2025 a las 11:15. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca."
echo Enviado: Notificacion Gmail - DiDi COP300
echo.
goto :end

:gmail-usd
echo === TEST: NOTIFICACION GMAIL - COMPRA USD ===
adb shell cmd notification post -t "Bancolombia" "bancolombia_gmail_002" "Bancolombia: Compraste USD1,00 en LOUNGEKEY con tu T.Cred *2670, el 20/12/2025 a las 17:47. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca."
echo Enviado: Notificacion Gmail - LoungeKey USD1
echo.
goto :end

:all
echo === EJECUTANDO TODOS LOS TESTS ===
echo.
call :compra-cop
call :compra-usd
call :transferencia
call :retiro
call :ingreso
call :nomina
call :gmail-cop
call :gmail-usd
echo === TODOS LOS TESTS COMPLETADOS ===
goto :end

:usage
echo Uso: %~nx0 [tipo]
echo.
echo Tipos disponibles:
echo   compra-cop      - Compra en pesos colombianos
echo   compra-usd      - Compra en dolares (moneda extranjera)
echo   transferencia   - Transferencia entre cuentas
echo   retiro          - Retiro de cajero automatico
echo   ingreso         - Transferencia recibida
echo   nomina          - Pago de nomina
echo   gmail-cop       - Notificacion de Gmail con compra COP
echo   gmail-usd       - Notificacion de Gmail con compra USD
echo   all             - Ejecutar todos los tests
echo.
echo Ejemplo: %~nx0 compra-cop
goto :end

:end
echo Para ver los logs en tiempo real, ejecuta:
echo adb logcat SmsNotifListener:D BancolombiaParser:D *:S
endlocal
