# 1. Grupo de Usuarios (User Pool)
resource "aws_cognito_user_pool" "pool" {
  name                     = "dsy1107-grupo007"
  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  password_policy {
    minimum_length    = 8
    require_lowercase = true
    require_uppercase = true
    require_numbers   = true
    require_symbols   = false
  }

  admin_create_user_config {
    allow_admin_create_user_only = true
  }
}

# 2. Dominio para la Hosted UI
resource "aws_cognito_user_pool_domain" "hosted_ui" {
  domain       = "dsy1107-grupo007"
  user_pool_id = aws_cognito_user_pool.pool.id
}

# 3. Cliente de la Aplicación (SPA React)
resource "aws_cognito_user_pool_client" "spa" {
  name         = "spa-react"
  user_pool_id = aws_cognito_user_pool.pool.id

  generate_secret = false

  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  supported_identity_providers          = ["COGNITO"]
  
  # Scopes requeridos por el laboratorio
  allowed_oauth_scopes = ["openid", "email", "profile", "aws.cognito.signin.user.admin"]

  callback_urls = ["http://localhost:5173/"]
  logout_urls   = ["http://localhost:5173/"]

  explicit_auth_flows = ["ALLOW_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"]

  access_token_validity = 60
  id_token_validity     = 60

  token_validity_units {
    access_token = "minutes"
    id_token     = "minutes"
  }
}

# 4. Usuario Demo Inicial
resource "aws_cognito_user" "demo" {
  user_pool_id = aws_cognito_user_pool.pool.id
  username     = "DovahZenin@gmail.com"
  password     = "Dovahzenin123456789"

  attributes = {
    email          = "DovahZenin@gmail.com"
    email_verified = true
    name           = "DovahZenin"
  }
  
  message_action = "SUPPRESS"
}