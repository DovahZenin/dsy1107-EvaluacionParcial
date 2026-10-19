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
                              │
                    JwtDecoder propio: firma + exp + issuer + audience
                    @PreAuthorize: scopes en todas las rutas
```

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
3. API con Postman (base: `https://jidj8317pa.execute-api.us-east-1.amazonaws.com`):
   - `GET /api/solicitudes/todas` (scope `solicitudes/approve`)
   - `GET /api/solicitudes/mis-solicitudes` (scope `solicitudes/read`)
   - Sin token → 401; token sin scope → 403 (esto es lo que demuestra la validación).

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
