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

# 4. Integración HTTP hacia el Backend
resource "aws_apigatewayv2_integration" "backend" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "HTTP_PROXY"
  # Se inicializa con un valor temporal; el script de ECS escribirá la IP real al desplegar
  integration_uri        = "http://127.0.0.1:8080/dummy" 
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
  target               = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_type   = "JWT"
  authorization_scopes = ["solicitudes/read"]
}

resource "aws_apigatewayv2_route" "post_solicitudes" {
  api_id               = aws_apigatewayv2_api.http_api.id
  route_key            = "POST /api/solicitudes"
  target               = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_type   = "JWT"
  authorization_scopes = ["solicitudes/write"]
}

resource "aws_apigatewayv2_route" "delete_solicitudes" {
  api_id               = aws_apigatewayv2_api.http_api.id
  route_key            = "DELETE /api/solicitudes/{id}"
  target               = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_type   = "JWT"
  authorization_scopes = ["solicitudes/write"]
}

# --- RUTAS APROBADOR ---
resource "aws_apigatewayv2_route" "get_todas" {
  api_id               = aws_apigatewayv2_api.http_api.id
  route_key            = "GET /api/solicitudes/todas"
  target               = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_type   = "JWT"
  authorization_scopes = ["solicitudes/approve"]
}

resource "aws_apigatewayv2_route" "patch_evaluar" {
  api_id               = aws_apigatewayv2_api.http_api.id
  route_key            = "PATCH /api/solicitudes/{id}/evaluar"
  target               = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_type   = "JWT"
  authorization_scopes = ["solicitudes/approve"]
}

data "aws_region" "current" {}