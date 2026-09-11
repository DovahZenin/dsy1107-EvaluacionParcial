output "api_gateway_url" {
  description = "URL pública del API Gateway HTTP"
  value       = aws_apigatewayv2_api.http_api.api_endpoint
}

output "cognito_client_id" {
  value       = aws_cognito_user_pool_client.spa.id
  description = "ID del cliente de aplicación para el frontend"
}