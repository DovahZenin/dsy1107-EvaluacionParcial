# 1. API Gateway HTTP API
resource "aws_apigatewayv2_api" "http_api" {
  name          = "dsy1107-api-gateway"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
    allow_headers = ["Authorization", "Content-Type", "X-User-Id"]
    max_age       = 300
  }
}

# 2. Stage por defecto con Auto-Deploy
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.http_api.id
  name        = "$default"
  auto_deploy = true
}

# 3. Authorizer con Cognito (JWT)
resource "aws_apigatewayv2_authorizer" "cognito" {
  api_id           = aws_apigatewayv2_api.http_api.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "cognito-authorizer"

  jwt_configuration {
    audience = [aws_cognito_user_pool_client.spa.id]
    issuer = "https://cognito-idp.${data.aws_region.current.region}.amazonaws.com/${aws_cognito_user_pool.pool.id}"
  }
}

# 4. Integraciones HTTP hacia el Backend.
#
# Cada ruta tiene SU integración con el path completo en integration_uri: el
# API Gateway HTTP reenvía una petición a la URI de integración tal cual
# (sustituyendo los path-variables como {id}), NO le pega la ruta de la
# request delante. Por eso no basta con "http://IP:8080": el backend recibiría
# "/" y contestaría 404.
#
# La IP real la escribe el script de ECS al desplegar (la task es efímera);
# integration_uri se inicializa con un placeholder y queda fuera del control
# de Terraform (ignore_changes) para que el script pueda re-apuntarlo.
locals {
  backend_base = "http://127.0.0.1:8080"
}

resource "aws_apigatewayv2_integration" "get_mis_solicitudes" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "${local.backend_base}/api/solicitudes/mis-solicitudes"
  integration_method     = "ANY"
  payload_format_version = "1.0"

  lifecycle {
    ignore_changes = [integration_uri]
  }
}

resource "aws_apigatewayv2_integration" "post_solicitudes" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "${local.backend_base}/api/solicitudes"
  integration_method     = "ANY"
  payload_format_version = "1.0"

  lifecycle {
    ignore_changes = [integration_uri]
  }
}

resource "aws_apigatewayv2_integration" "delete_solicitudes" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "${local.backend_base}/api/solicitudes/{id}"
  integration_method     = "ANY"
  payload_format_version = "1.0"

  lifecycle {
    ignore_changes = [integration_uri]
  }
}

resource "aws_apigatewayv2_integration" "get_todas" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "${local.backend_base}/api/solicitudes/todas"
  integration_method     = "ANY"
  payload_format_version = "1.0"

  lifecycle {
    ignore_changes = [integration_uri]
  }
}

resource "aws_apigatewayv2_integration" "patch_evaluar" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "${local.backend_base}/api/solicitudes/{id}/evaluar"
  integration_method     = "ANY"
  payload_format_version = "1.0"

  lifecycle {
    ignore_changes = [integration_uri]
  }
}

# 5. Rutas específicas con sus respectivos scopes

# --- RUTAS SOLICITANTE ---
resource "aws_apigatewayv2_route" "get_mis_solicitudes" {
  api_id               = aws_apigatewayv2_api.http_api.id
  route_key            = "GET /api/solicitudes/mis-solicitudes"
  target               = "integrations/${aws_apigatewayv2_integration.get_mis_solicitudes.id}"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_type   = "JWT"
  authorization_scopes = ["solicitudes/read"]
}

resource "aws_apigatewayv2_route" "post_solicitudes" {
  api_id               = aws_apigatewayv2_api.http_api.id
  route_key            = "POST /api/solicitudes"
  target               = "integrations/${aws_apigatewayv2_integration.post_solicitudes.id}"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_type   = "JWT"
  authorization_scopes = ["solicitudes/write"]
}

resource "aws_apigatewayv2_route" "delete_solicitudes" {
  api_id               = aws_apigatewayv2_api.http_api.id
  route_key            = "DELETE /api/solicitudes/{id}"
  target               = "integrations/${aws_apigatewayv2_integration.delete_solicitudes.id}"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_type   = "JWT"
  authorization_scopes = ["solicitudes/write"]
}

# --- RUTAS APROBADOR ---
resource "aws_apigatewayv2_route" "get_todas" {
  api_id               = aws_apigatewayv2_api.http_api.id
  route_key            = "GET /api/solicitudes/todas"
  target               = "integrations/${aws_apigatewayv2_integration.get_todas.id}"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_type   = "JWT"
  authorization_scopes = ["solicitudes/approve"]
}

resource "aws_apigatewayv2_route" "patch_evaluar" {
  api_id               = aws_apigatewayv2_api.http_api.id
  route_key            = "PATCH /api/solicitudes/{id}/evaluar"
  target               = "integrations/${aws_apigatewayv2_integration.patch_evaluar.id}"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_type   = "JWT"
  authorization_scopes = ["solicitudes/approve"]
}

data "aws_region" "current" {}