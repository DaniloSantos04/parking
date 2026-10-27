#!/usr/bin/env bash
# Sobe o simulador de garagem do enunciado (cfontes0estapar/garage-sim:1.0.0).
#
# Uso: ./scripts/start-simulator.sh [host|port]
#   host  (padrão) comando do enunciado, com --network="host".
#         No Docker Desktop exige "Enable host networking" (Settings > Resources > Network).
#   port  plano B: publica a porta 3000 e envia o webhook para host.docker.internal:3003.
#
# O simulador só começa a enviar eventos depois da primeira chamada a GET /garage,
# que a aplicação faz ao subir.
set -euo pipefail

MODE="${1:-host}"
IMAGE="cfontes0estapar/garage-sim:1.0.0"
NAME="garage-sim"

docker rm -f "$NAME" >/dev/null 2>&1 || true

case "$MODE" in
  host)
    docker run -d --name "$NAME" --network="host" "$IMAGE"
    ;;
  port)
    docker run -d --name "$NAME" -p 3000:3000 \
      -e CLIENT_WEBHOOK_URL=http://host.docker.internal:3003/webhook \
      "$IMAGE"
    ;;
  *)
    echo "Uso: $0 [host|port]" >&2
    exit 1
    ;;
esac

echo "Simulador no modo '$MODE': GET http://localhost:3000/garage"
echo "Eventos enviados para a aplicação em http://localhost:3003/webhook"
echo "Logs: docker logs -f $NAME | Parar: docker rm -f $NAME"
