output "cognito_client_id" {
  value       = aws_cognito_user_pool_client.spa.id
  description = "ID del cliente de aplicación para el frontend"
}