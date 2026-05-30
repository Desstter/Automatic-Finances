#!/bin/bash
# Script de testing para notificaciones de AutomaticFinances
# Uso: ./test-notifications.sh [tipo]
# Tipos disponibles: compra-cop, compra-usd, transferencia, retiro, ingreso, nomina, gmail-cop, gmail-usd, all

# Colores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Verificar que ADB está disponible
if ! command -v adb &> /dev/null; then
    echo "Error: ADB no está instalado o no está en el PATH"
    exit 1
fi

# Verificar que hay un dispositivo conectado
DEVICE=$(adb devices | grep -w "device" | head -n 1)
if [ -z "$DEVICE" ]; then
    echo "Error: No hay dispositivos conectados. Conecta un dispositivo o emulador."
    exit 1
fi

echo -e "${GREEN}Dispositivo detectado:${NC} $DEVICE"
echo ""

# Función para enviar SMS simulado
send_sms() {
    local phone=$1
    local message=$2
    local description=$3

    echo -e "${BLUE}Enviando:${NC} $description"
    adb emu sms send "$phone" "$message"
    echo -e "${GREEN}✓ Enviado${NC}"
    echo ""
    sleep 1
}

# Función para enviar notificación de app
send_notification() {
    local title=$1
    local tag=$2
    local message=$3
    local description=$4

    echo -e "${BLUE}Enviando notificación:${NC} $description"
    adb shell cmd notification post -t "$title" "$tag" "$message"
    echo -e "${GREEN}✓ Enviado${NC}"
    echo ""
    sleep 1
}

# Casos de prueba
case "$1" in
    compra-cop)
        echo -e "${YELLOW}=== TEST: COMPRA EN PESOS (COP) ===${NC}"
        send_sms "+573001234567" \
            "Bancolombia: Compraste COP50.000,00 en RAPPI con tu T.Cred *2670, el 20/12/2025 a las 14:30. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca." \
            "Compra Rappi COP50.000"
        ;;

    compra-usd)
        echo -e "${YELLOW}=== TEST: COMPRA EN DÓLARES (USD) ===${NC}"
        send_sms "+573001234567" \
            "Bancolombia: Compraste USD25,50 en AMAZON WEB SERVICES con tu T.Cred *2670, el 20/12/2025 a las 16:45. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca." \
            "Compra AWS USD25.50"
        ;;

    transferencia)
        echo -e "${YELLOW}=== TEST: TRANSFERENCIA ===${NC}"
        send_sms "+573001234567" \
            "Bancolombia: Transferiste COP100.000,00 desde tu cuenta *2670 a la cuenta *5678 el 20/12/2025 a las 10:15. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca." \
            "Transferencia COP100.000"
        ;;

    retiro)
        echo -e "${YELLOW}=== TEST: RETIRO CAJERO ===${NC}"
        send_sms "+573001234567" \
            "Bancolombia: Retiraste COP200.000,00 en CAJERO_EXITO_80 de tu T.Deb *2670 el 20/12/2025 a las 18:20. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca." \
            "Retiro cajero COP200.000"
        ;;

    ingreso)
        echo -e "${YELLOW}=== TEST: INGRESO/TRANSFERENCIA RECIBIDA ===${NC}"
        send_sms "+573001234567" \
            "Bancolombia: Recibiste COP150.000,00 a tu cuenta *2670 el 20/12/2025 a las 09:00. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca." \
            "Ingreso recibido COP150.000"
        ;;

    nomina)
        echo -e "${YELLOW}=== TEST: PAGO DE NÓMINA ===${NC}"
        send_sms "+573001234567" \
            "Bancolombia: Recibiste un pago de Nómina de EMPRESA TECH SAS por COP3.500.000,00 en tu cuenta de Ahorros *2670 el 01/12/2025 a las 08:00. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca." \
            "Nómina COP3.500.000"
        ;;

    gmail-cop)
        echo -e "${YELLOW}=== TEST: NOTIFICACIÓN GMAIL - COMPRA COP ===${NC}"
        send_notification "Bancolombia" "bancolombia_gmail_001" \
            "Bancolombia: Compraste COP300,00 en DLO*DiDi CO Payin (R con tu T.Cred *2670, el 20/12/2025 a las 11:15. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca." \
            "Notificación Gmail - DiDi COP300"
        ;;

    gmail-usd)
        echo -e "${YELLOW}=== TEST: NOTIFICACIÓN GMAIL - COMPRA USD ===${NC}"
        send_notification "Bancolombia" "bancolombia_gmail_002" \
            "Bancolombia: Compraste USD1,00 en LOUNGEKEY con tu T.Cred *2670, el 20/12/2025 a las 17:47. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca." \
            "Notificación Gmail - LoungeKey USD1"
        ;;

    all)
        echo -e "${YELLOW}=== EJECUTANDO TODOS LOS TESTS ===${NC}"
        echo ""
        $0 compra-cop
        $0 compra-usd
        $0 transferencia
        $0 retiro
        $0 ingreso
        $0 nomina
        $0 gmail-cop
        $0 gmail-usd
        echo -e "${GREEN}=== TODOS LOS TESTS COMPLETADOS ===${NC}"
        ;;

    *)
        echo "Uso: $0 [tipo]"
        echo ""
        echo "Tipos disponibles:"
        echo "  compra-cop      - Compra en pesos colombianos"
        echo "  compra-usd      - Compra en dólares (moneda extranjera)"
        echo "  transferencia   - Transferencia entre cuentas"
        echo "  retiro          - Retiro de cajero automático"
        echo "  ingreso         - Transferencia recibida"
        echo "  nomina          - Pago de nómina"
        echo "  gmail-cop       - Notificación de Gmail con compra COP"
        echo "  gmail-usd       - Notificación de Gmail con compra USD"
        echo "  all             - Ejecutar todos los tests"
        echo ""
        echo "Ejemplo: $0 compra-cop"
        exit 1
        ;;
esac

echo -e "${BLUE}Para ver los logs en tiempo real, ejecuta:${NC}"
echo "adb logcat SmsNotifListener:D BancolombiaParser:D *:S"
