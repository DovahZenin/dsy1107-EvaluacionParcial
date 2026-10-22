#!/usr/bin/env bash
#
# Conecta el backend de la nube (ECS) con el RabbitMQ LOCAL via Tailscale.
#
# PROBLEMA QUE RESUELVE
#   En AWS no hay broker (ver terraform/ecs.tf): el backend publica eventos y
#   solo registra un warning. Para la demo se quiere que esos eventos lleguen al
#   Rabbit que corre en Docker en el equipo del estudiante.
#
# COMO
#   Se agrega un contenedor sidecar `tailscale` a la MISMA task de Fargate que el
#   backend. Como la task usa red awsvpc, el sidecar comparte red con el backend
#   (se ven en 127.0.0.1). Fargate no da NET_ADMIN, asi que Tailscale corre en
#   modo userspace y publica un proxy SOCKS5 en 127.0.0.1:1055; el backend lo usa
#   via JAVA_TOOL_OPTIONS para que las conexiones AMQP salgan por la tailnet.
#   El trafico va cifrado (WireGuard) de extremo a extremo.
#
#   Tailscale ve al equipo local por su IP estable de la tailnet (p. ej.
#   100.x.y.z), asi que esta configuracion se registra UNA vez; no cambia como la
#   IP publica de la task.
#
# USO
#   ./scripts/conectar-rabbit-local.sh configurar <ip-tailscale>
#   ./scripts/conectar-rabbit-local.sh apagar
#
#   Las credenciales (authkey de Tailscale y usuario/password de Rabbit) se leen
#   de credenciales-lab.env (en .gitignore), o de variables de entorno:
#   TS_AUTHKEY, RABBIT_USER, RABBIT_PASS.

set -euo pipefail

REGION="${AWS_REGION:-us-east-1}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# terraform, aws y docker son binarios nativos de Windows: si se les pasa una
# ruta tipo /c/Users/... no la entienden (y Git Bash, a su vez, convierte los
# argumentos "/algo" en rutas de Windows). Para que el script funcione igual en
# Windows (Git Bash) y en Linux/macOS:
#   - RAIZ_OS: la raiz en formato nativo, solo para terraform.
#   - Los pocos literales con "/" que NO deben convertirse (vhost de Rabbit,
#     nombre del log group "/ecs/...") se marcan con la variable de exclusion.
RAIZ_OS="$RAIZ"
if command -v cygpath >/dev/null 2>&1; then
  RAIZ_OS="$(cygpath -w "$RAIZ")"
fi
TF="terraform -chdir=${RAIZ_OS}/terraform"

# ---------------------------------------------------------------------------
# Configuracion: del archivo local o del entorno.
# ---------------------------------------------------------------------------
if [ -f "${RAIZ}/credenciales-lab.env" ]; then
  # shellcheck disable=SC1091
  set -a; . "${RAIZ}/credenciales-lab.env"; set +a
fi

RABBIT_USER="${RABBIT_USER:-nube}"
RABBIT_PASS="${RABBIT_PASS:-}"
RABBIT_PORT="${RABBIT_PORT:-5672}"
TS_AUTHKEY="${TS_AUTHKEY:-}"

ACCION="${1:-}"
IP_TAILSCALE="${2:-}"

leer() {  # $1 = var de entorno, $2 = output de terraform
  local desde_entorno="${!1:-}"
  if [ -n "$desde_entorno" ]; then echo "$desde_entorno"
  elif command -v terraform >/dev/null 2>&1 && $TF output -raw "$2" >/dev/null 2>&1; then $TF output -raw "$2"
  else echo "Falta \$$1 y no hay salida '$2' en Terraform." >&2; return 1; fi
}
leer_json() {
  local desde_entorno="${!1:-}"
  if [ -n "$desde_entorno" ]; then echo "$desde_entorno"
  elif command -v terraform >/dev/null 2>&1 && $TF output -json "$2" >/dev/null 2>&1; then $TF output -json "$2"
  else echo "Falta \$$1 y no hay salida '$2' en Terraform." >&2; return 1; fi
}

command -v jq >/dev/null || { echo "Falta jq." >&2; exit 1; }

CLUSTER="$(leer ECS_CLUSTER ecs_cluster)"
SERVICIO="$(leer ECS_SERVICE ecs_servicio)"
API_ID="$(leer API_ID api_id)"
INTEGRACIONES_JSON="$(leer_json API_INTEGRACIONES integraciones_id)"

# ---------------------------------------------------------------------------
# Reapuntar el API Gateway: cada redespliegue crea una task con IP publica nueva.
# (Mismo procedimiento que publicar-ecs.sh.)
# ---------------------------------------------------------------------------
reapuntar_gateway() {
  echo "==> Reapuntando el API Gateway a la task nueva"
  local TAREA ENI IP
  TAREA="$(aws ecs list-tasks --region "$REGION" --cluster "$CLUSTER" \
    --service-name "$SERVICIO" --desired-status RUNNING \
    --query "taskArns[0]" --output text)"
  ENI="$(aws ecs describe-tasks --region "$REGION" --cluster "$CLUSTER" --tasks "$TAREA" \
    --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" --output text)"
  IP="$(aws ec2 describe-network-interfaces --region "$REGION" --network-interface-ids "$ENI" \
    --query "NetworkInterfaces[0].Association.PublicIp" --output text)"
  if [ -z "$IP" ] || [ "$IP" = "None" ]; then
    echo "No se pudo resolver la IP publica de la task ($TAREA)." >&2; exit 1
  fi
  local id
  for par in \
    "mis_solicitudes:/api/solicitudes/mis-solicitudes" \
    "post:/api/solicitudes" \
    "delete:/api/solicitudes/{id}" \
    "todas:/api/solicitudes/todas" \
    "evaluar:/api/solicitudes/{id}/evaluar"; do
    id="$(echo "$INTEGRACIONES_JSON" | jq -r --arg k "${par%%:*}" '.[$k] // empty')"
    [ -n "$id" ] || continue
    aws apigatewayv2 update-integration --region "$REGION" --api-id "$API_ID" \
      --integration-id "$id" --integration-uri "http://${IP}:8080${par#*:}" >/dev/null
  done
  echo "    gateway -> http://${IP}:8080"
  echo "    backend directo: http://${IP}:8080 (devuelve 401 sin token)"
}

# ---------------------------------------------------------------------------
# Registrar una revision nueva y esperar a que el servicio converja.
# ---------------------------------------------------------------------------
redesplegar() {
  local TAREA_JSON
  TAREA_JSON="$(cat)"
  local REVISION
  REVISION="$(aws ecs register-task-definition --region "$REGION" \
    --cli-input-json "$TAREA_JSON" \
    --query "taskDefinition.taskDefinitionArn" --output text)"
  echo "    revision ${REVISION##*/} registrada"
  aws ecs update-service --region "$REGION" --cluster "$CLUSTER" \
    --service "$SERVICIO" --task-definition "$REVISION" >/dev/null

  echo "    Esperando a que el despliegue termine..."
  local INTENTO LEIDO ESTADO CORRIENDO DESEADAS
  INTENTO=0
  while [ "$INTENTO" -lt 60 ]; do
    INTENTO=$((INTENTO+1))
    if ! LEIDO="$(aws ecs describe-services --region "$REGION" --cluster "$CLUSTER" \
      --services "$SERVICIO" \
      --query "services[0].[deployments[0].rolloutState,runningCount,desiredCount]" \
      --output text 2>&1)"; then
      echo "No se pudo consultar el servicio: $LEIDO" >&2; exit 1
    fi
    ESTADO="$(echo "$LEIDO" | awk '{print $1}')"
    CORRIENDO="$(echo "$LEIDO" | awk '{print $2}')"
    DESEADAS="$(echo "$LEIDO" | awk '{print $3}')"
    echo "    ${ESTADO}  ${CORRIENDO}/${DESEADAS}"
    if [ "$ESTADO" = "COMPLETED" ] && [ "$CORRIENDO" = "$DESEADAS" ]; then break; fi
    if [ "$ESTADO" = "FAILED" ]; then
      echo "FALLO el despliegue. Logs:" >&2
      echo "    aws logs tail /ecs/${SERVICIO} --follow" >&2
      exit 1
    fi
    [ "$INTENTO" -eq 60 ] && { echo "Se agoto la espera." >&2; exit 1; }
    sleep 10
  done
}

# ---------------------------------------------------------------------------
# Traer la task definition actual, lista para editar con jq.
# ---------------------------------------------------------------------------
traer_task_json() {
  aws ecs describe-task-definition --region "$REGION" \
    --task-definition "$SERVICIO" --query taskDefinition --output json \
    | jq 'del(.taskDefinitionArn, .revision, .status, .requiresAttributes,
              .compatibilities, .registeredAt, .registeredBy, .deregisteredAt)'
}

case "$ACCION" in
  configurar)
    if [ -z "$IP_TAILSCALE" ]; then
      echo "Uso: $0 configurar <ip-tailscale>" >&2
      echo "  (la IP de este equipo en la tailnet: tailscale ip -4)" >&2
      exit 1
    fi
    [ -n "$TS_AUTHKEY" ] || { echo "Falta TS_AUTHKEY (en credenciales-lab.env)." >&2; exit 1; }
    [ -n "$RABBIT_PASS" ] || { echo "Falta RABBIT_PASS (en credenciales-lab.env)." >&2; exit 1; }

    echo "==> 1/4 Creando usuario Rabbit '${RABBIT_USER}' con permisos acotados"

    # guest solo vale desde localhost; el backend llega por la tailnet, asi que
    # necesita un usuario propio. Permisos: solo sobre el exchange del sistema.
    docker exec dsy1107-rabbitmq rabbitmqctl add_user "$RABBIT_USER" "$RABBIT_PASS" 2>/dev/null \
      || docker exec dsy1107-rabbitmq rabbitmqctl change_password "$RABBIT_USER" "$RABBIT_PASS"
    # MSYS_NO_PATHCONV=1 evita que Git Bash convierta el vhost "/" en una ruta de
    # Windows. En Linux/macOS la variable no existe y no molesta.
    MSYS_NO_PATHCONV=1 docker exec dsy1107-rabbitmq rabbitmqctl set_permissions -p / "$RABBIT_USER" \
      "^(solicitudes\\.topic|solicitudes\\.dlx)$" "^(solicitudes\\.topic|solicitudes\\.dlx)$" ".*"
    echo "    usuario listo"

    echo "==> 2/4 Construyendo la task definition (sidecar Tailscale + env Rabbit)"

    # JAVA_TOOL_OPTIONS: hace que TODA conexion saliente del JVM pase por el
    # SOCKS5 de tailscaled. Lo que no es de la tailnet lo relay a internet normal.
    # -Djava.net.useSystemProxies=true no basta: SOCKS no es proxy del sistema
    # para todos los protocolos, por eso se fija explicitamente.
    SOCKS_OPTS="-DsocksProxyHost=127.0.0.1 -DsocksProxyPort=1055"

    # El nombre del log group empieza con "/", que Git Bash convertiria en una
    # ruta de Windows al pasarlo como argumento de jq. Se pasa SIN el slash y el
    # slash se agrega dentro de jq con ("/" + $logs).
    LOG_SUFFIX="ecs/${SERVICIO}"

    TAREA_JSON="$(traer_task_json | jq \
      --arg ts "$TS_AUTHKEY" \
      --arg host "$IP_TAILSCALE" \
      --arg port "$RABBIT_PORT" \
      --arg user "$RABBIT_USER" \
      --arg pass "$RABBIT_PASS" \
      --arg socks "$SOCKS_OPTS" \
      --arg logs "$LOG_SUFFIX" \
      --arg region "$REGION" '
      # 1. Env del backend: upsert de las variables de Rabbit + proxy SOCKS.
      #    Se conservan las de RDS/Cognito que ya trae la task.
      (.containerDefinitions[] | select(.name == "backend")
        | .environment) |= (
          map(select(.name as $n
            | ["SPRING_RABBITMQ_HOST","SPRING_RABBITMQ_PORT","SPRING_RABBITMQ_USERNAME",
               "SPRING_RABBITMQ_PASSWORD","JAVA_TOOL_OPTIONS"] | index($n) | not))
          + [
              {name:"SPRING_RABBITMQ_HOST",     value:$host},
              {name:"SPRING_RABBITMQ_PORT",     value:$port},
              {name:"SPRING_RABBITMQ_USERNAME", value:$user},
              {name:"SPRING_RABBITMQ_PASSWORD", value:$pass},
              {name:"JAVA_TOOL_OPTIONS",        value:$socks}
            ]
        )
      # 2. Memoria de la task a 2048 MB (Fargate exige que el sidecar quepa).
      | .memory = "2048"
      # 3. Quitar cualquier sidecar previo (idempotente) y agregar el nuevo.
      | .containerDefinitions |= map(select(.name != "tailscale"))
      | .containerDefinitions += [{
          name: "tailscale",
          image: "tailscale/tailscale:stable",
          essential: false,
          memoryReservation: 256,
          linuxParameters: {initProcessEnabled: true},
          environment: [
            {name:"TS_AUTHKEY",     value:$ts},
            {name:"TS_HOSTNAME",    value:"dsy1107-backend-task"},
            {name:"TS_USERSPACE",   value:"true"},
            {name:"TS_STATE_DIR",   value:"/tmp/tailscaled"},
            {name:"TS_SOCKS5_SERVER","value":"127.0.0.1:1055"}
          ],
          logConfiguration: {
            logDriver: "awslogs",
            options: {
              "awslogs-group": ("/" + $logs),
              "awslogs-region": $region,
              "awslogs-stream-prefix": "tailscale"
            }
          }
        }]')"

    echo "$TAREA_JSON" | redesplegar
    reapuntar_gateway

    echo
    echo "OK  puente activo."
    echo "    La nube publicara los eventos en el Rabbit de ${IP_TAILSCALE}:${RABBIT_PORT} (via tailnet, cifrado)."
    echo "    Recuerda tener levantado en local: docker compose + notificaciones-service."
    ;;

  apagar)
    echo "==> Quitando el puente (volver a 'sin broker')"
    TAREA_JSON="$(traer_task_json | jq '
      # Quitar el sidecar y las variables de Rabbit/proxy del backend.
      (.containerDefinitions[] | select(.name == "backend") | .environment) |=
        map(select(.name as $n
          | ["SPRING_RABBITMQ_HOST","SPRING_RABBITMQ_PORT","SPRING_RABBITMQ_USERNAME",
             "SPRING_RABBITMQ_PASSWORD","JAVA_TOOL_OPTIONS"] | index($n) | not))
      | .containerDefinitions |= map(select(.name != "tailscale"))
      | .memory = "1024"')"

    echo "$TAREA_JSON" | redesplegar
    reapuntar_gateway
    echo "OK  puente desactivado (el backend vuelve a degradar a warning)."
    ;;

  *)
    echo "Uso:" >&2
    echo "  $0 configurar <ip-tailscale>   conecta la nube al Rabbit local" >&2
    echo "  $0 apagar                      desconecta (rollback)" >&2
    exit 1
    ;;
esac
