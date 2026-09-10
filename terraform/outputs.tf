output "api_gateway_url" {
  description = "URL pública del API Gateway HTTP"
  value       = aws_apigatewayv2_api.http_api.api_endpoint
}