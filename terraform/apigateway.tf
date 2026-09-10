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
  api_id           = aws_apigatewayv2_api.http_api.id
  integration_type = "HTTP_PROXY"
  
  # API Gateway exige una URL pública enrutable (HTTP o HTTPS).
  # Para pruebas locales de IaC usa este endpoint temporal. 
  # En producción con ECS/EC2 se reemplaza por http://<DNS_DEL_ALB>
  integration_uri  = "https://httpbin.org/anything"
  
  integration_method     = "ANY"
  payload_format_version = "1.0"
}

# 5. Ruta genérica con autenticación para /api/solicitudes
resource "aws_apigatewayv2_route" "solicitudes" {
  api_id             = aws_apigatewayv2_api.http_api.id
  route_key          = "ANY /api/solicitudes/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito.id
  authorization_type = "JWT"
}

data "aws_region" "current" {}