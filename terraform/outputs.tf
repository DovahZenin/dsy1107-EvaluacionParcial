output "api_gateway_url" {
  description = "URL pública del API Gateway HTTP"
  value       = aws_apigatewayv2_api.http_api.api_endpoint
}

output "cognito_client_id" {
  value       = aws_cognito_user_pool_client.spa.id
  description = "ID del cliente de aplicación para el frontend"
}

output "cognito_user_pool_id" {
  value       = aws_cognito_user_pool.pool.id
  description = "ID del User Pool de Cognito (para issuer-uri del backend)"
}

output "ecs_repositorio" {
  value       = aws_ecr_repository.backend.repository_url
  description = "URL del repositorio ECR donde se sube la imagen del backend"
}

output "ecs_cluster" {
  value       = aws_ecs_cluster.main.name
  description = "Nombre del cluster ECS"
}

output "ecs_servicio" {
  value       = aws_ecs_service.backend.name
  description = "Nombre del servicio ECS (y familia de la task definition)"
}

output "api_id" {
  value       = aws_apigatewayv2_api.http_api.id
  description = "ID del API Gateway HTTP (para reapuntar la integración)"
}

output "integraciones_id" {
  value = {
    mis_solicitudes = aws_apigatewayv2_integration.get_mis_solicitudes.id
    post            = aws_apigatewayv2_integration.post_solicitudes.id
    delete          = aws_apigatewayv2_integration.delete_solicitudes.id
    todas           = aws_apigatewayv2_integration.get_todas.id
    evaluar         = aws_apigatewayv2_integration.patch_evaluar.id
  }
  description = "IDs de las integraciones HTTP hacia el backend (reapuntar al desplegar)"
}