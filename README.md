# DSY1107-EA1 — Sistema de Gestión de Solicitudes

Aplicación full-stack para crear, listar y evaluar solicitudes de colaboradores
(vacaciones, permisos, licencias, trabajo remoto).

- **Frontend**: React (Vite) con login OIDC + PKCE contra Amazon Cognito.
- **API**: API Gateway HTTP con autorizador JWT de Cognito.
- **Backend**: Spring Boot (Java 17) corriendo en ECS Fargate.
- **Base de datos**: PostgreSQL en Amazon RDS.

## Arquitectura

```
Navegador (React, localhost:5173)
   │  login OIDC+PKCE          │  Bearer token (access_token)
   ▼                           ▼
Cognito (Hosted UI)      API Gateway (autorizador JWT: firma, issuer, audience)
                              │  rutas con scopes por rol
                              ▼
                        ECS Fargate (Spring Boot)  ──▶  RDS PostgreSQL
                              │  publica evento solicitud.creada
                              │  (sidecar Tailscale, proxy SOCKS5 127.0.0.1:1055)
                              ▼  tailnet (WireGuard)
                  RabbitMQ local (Docker)  ──▶  notificaciones-service / admin-rabbit-service
```

En el backend, un `JwtDecoder` propio valida firma + `exp` + issuer + audience, y
`@PreAuthorize` exige scopes en todas las rutas.

Una Lambda de **Pre Token Generation** (`user-token-ms/`) inyecta scopes por
grupo: `solicitantes` → `solicitudes/read`, `solicitudes/write`;
`aprobadores` → además `solicitudes/approve`.

## Cómo levantarla en local

Solo frontend (no requiere backend local, la API vive en AWS):

```bash
git clone https://github.com/DovahZenin/dsy1107-EvaluacionParcial
cd dsy1107-EvaluacionParcial/frontend
npm ci
npm run dev          # http://localhost:5173
```

Backend opcional (requiere acceso al RDS):

```bash
cd backend
./mvnw spring-boot:run    # Windows: .\mvnw.cmd spring-boot:run
```

## Flujo de autenticación (OIDC + PKCE)

1. `login()` (`auth.js`) redirige a `/oauth2/authorize` con `code_challenge` S256.
2. Cognito autentica y redirige a `http://localhost:5173/?code=...&state=...`.
3. `procesarRetorno()` limpia la URL y canjea `code + code_verifier` en
   `/oauth2/token` (`grant_type=authorization_code`). Es idempotente: no canjea
   el código dos veces aunque StrictMode ejecute el efecto en doble.
4. Si el access token expira (60 min), el frontend lo renueva en silencio con
   `refresh_token` (`refrescarTokens()`); un 401 de la API cierra la sesión.
5. El access token viaja en `Authorization: Bearer` a cada endpoint.

## Validación del token en cada capa

| Capa | Qué valida |
|:-----|:-----------|
| API Gateway (authorizer JWT) | Firma (JWKS), issuer y audience (`terraform/apigateway.tf`). |
| Backend (bean `JwtDecoder` en `SecurityConfig`) | Firma, expiración (exp), issuer y audience obligatorios (`AudienceValidator`), en **todas** las rutas. |
| Backend (`@PreAuthorize`) | Scopes por ruta: `solicitudes/read`, `solicitudes/write`, `solicitudes/approve`. |
| Frontend | Lee scopes y `cognito:groups`; renueva token expirado; maneja 401/403. |

## Pruebas

```bash
cd backend
# Unitarias del servicio
.\mvnw.cmd test -Dtest=SolicitudServiceTest
# Validación de audience + seguridad de rutas (401 sin token, 403 sin scope, 200 con scope)
.\mvnw.cmd test -Dtest=AudienceValidatorTest,SolicitudControllerSecurityTest
# Todas excepto la de contexto (que necesita RDS)
.\mvnw.cmd test -Dtest='!*ApplicationTests'

cd ../frontend
npm run lint -- src
npm run build
```

## Probar el flujo completo

1. `npm run dev` → abrir `http://localhost:5173` → **Iniciar Sesión con Cognito**.
2. Solicitante: crear y ver sus solicitudes. Aprobador: panel de aprobación.
3. API con Postman (base: `https://vgit7wwug4.execute-api.us-east-1.amazonaws.com`):
   - `GET /api/solicitudes/todas` (scope `solicitudes/approve`)
   - `GET /api/solicitudes/mis-solicitudes` (scope `solicitudes/read`)
   - Sin token → 401; token sin scope → 403 (esto es lo que demuestra la validación).

## Mensajería: eventos de la nube hacia un RabbitMQ local

El backend publica un evento `solicitud.creada` (`RabbitPublicacionConfig` +
`SolicitudEventPublisher`) cada vez que se crea una solicitud. Para la demo, ese
evento viaja desde ECS hasta el **RabbitMQ que corre en Docker en el equipo**,
aunque no haya conectividad directa entre ambos:

- Un sidecar `tailscale/tailscale` corre en la misma task de Fargate que el
  backend. Como la task usa red `awsvpc`, ambos contenedores comparten la misma
  interfaz (se ven en `127.0.0.1`).
- Fargate no otorga `NET_ADMIN`, así que Tailscale corre en **modo userspace** y
  expone un **proxy SOCKS5** en `127.0.0.1:1055`.
- El backend recibe `JAVA_TOOL_OPTIONS=-DsocksProxyHost=127.0.0.1 -DsocksProxyPort=1055`,
  de modo que sus conexiones AMQP salen por la tailnet (cifradas con WireGuard)
  hasta la IP estable del equipo local. El tráfico que no es de la tailnet se
  sigue relayando a internet.

Como `guest` de RabbitMQ solo acepta conexiones desde `localhost`, el script crea
un usuario acotado (`nube`) con permisos solo sobre `solicitudes.topic` y
`solicitudes.dlx`.

### Encender el puente

```bash
# 1) Levantar el Rabbit local y los consumidores (declaran la topología)
docker compose up -d
# notificaciones-service y admin-rabbit-service (consumidores) ya corriendo

# 2) Conectar la nube al Rabbit local (la IP es la del equipo en la tailnet)
tailscale ip -4                                  # p. ej. 100.116.47.21
./scripts/conectar-rabbit-local.sh configurar 100.116.47.21

# 3) Crear una solicitud desde el frontend y ver el evento llegar en local:
#    consola de notificaciones-service → "[notificacion] ..." y "[auditoria] ..."
```

### Apagar el puente (rollback)

```bash
./scripts/conectar-rabbit-local.sh apagar
```

Sin el puente, el backend no falla: registra un `WARN` y sigue operando (la
publicación es *best-effort*, nunca bloquea la transacción de la base de datos).

### Eventos: rúbrica

- **Nombres centralizados**: colas, exchange y routing key viven en
  `RabbitPublicacionConfig` (publicador) y `RabbitTopologyConfig` (consumidores),
  sin strings sueltos.
- **Beans de topología**: `Queue`/`TopicExchange`/`Binding` con DLX y DLQ
  (`solicitudes.dlx` → `solicitudes.dlq`) declarados como beans.
- **ACK/DLQ**: el consumidor hace `ACK` manual; un payload inválido no se
  reencola en bucle, se enruta a la DLQ.
- **API de administración**: `admin-rabbit-service` expone crear/consultar/borrar
  colas (REST), validando el `QueueDTO` con Bean Validation (400 en payload
  inválido, 201 al crear, 409 si ya existe, 204 al borrar).

## Credenciales de prueba

| Rol | Email | Contraseña |
|:----|:------|:-----------|
| Solicitante | DovahZenin@gmail.com | Dovahzenin123456789 |
| Aprobador | Ryuma@gmail.com | Ryuma123456 |

## Despliegue (GitHub Actions)

| Workflow | Qué hace |
|:---------|:---------|
| `backend_deploy` | Build → imagen → push a ECR → actualiza servicio ECS → reapunta las 5 integraciones del API Gateway a la IP nueva (vars `API_ID` y `API_INTEGRACIONES`). |
| `frontend_deploy` | `npm ci` + lint + build (verificación; el hosting es local/localhost). |

Tras un `terraform apply` que recree el pool o el API, hay que actualizar
`frontend/src/config.js`, `app.oauth.*` en `application.properties` y las
variables de GitHub.

## Mapeo a la rúbrica

- **Autenticación (MSAL/Angular ↔ equivalente OIDC+PKCE con Cognito y React)**:
  flujo completo con PKCE, obtención de todos los tokens (access, id, refresh),
  renovación silenciosa, guards de permisos en la UI y lectura completa de
  roles (scopes + `cognito:groups`).
- **Validación de tokens (BFF ↔ API Gateway + backend como capa de validación)**:
  todas las rutas exigen token con firma, expiración, issuer y audience
  verificados, más los scopes correctos; cubierto también el acceso directo a
  la IP de la task. Pruebas automatizadas (`AudienceValidatorTest`,
  `SolicitudControllerSecurityTest`) lo demuestran.
