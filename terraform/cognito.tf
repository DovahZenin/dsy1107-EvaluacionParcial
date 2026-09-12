# 1. Grupo de Usuarios (User Pool)
resource "aws_cognito_user_pool" "pool" {
  name                     = "dsy1107-grupo8"
  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]
  
  user_pool_tier = "ESSENTIALS"

  password_policy {
    minimum_length    = 8
    require_lowercase = true
    require_uppercase = true
    require_numbers   = false
    require_symbols   = false
  }

  admin_create_user_config {
    allow_admin_create_user_only = true
  }

  lambda_config {
    pre_token_generation_config {
      lambda_arn     = aws_lambda_function.user_token_ms.arn
      lambda_version = "V2_0"
    }
  }
}

# 2. Dominio para la Hosted UI
resource "aws_cognito_user_pool_domain" "hosted_ui" {
  domain       = "dsy1107-grupo8-app"
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
  
  allowed_oauth_scopes = [
    "openid",
    "email",
    "profile",
    "aws.cognito.signin.user.admin",
    "solicitudes/read",
    "solicitudes/write",
    "solicitudes/approve",
  ]

  callback_urls = ["http://localhost:5173/"]
  logout_urls   = ["http://localhost:5173/"]

  explicit_auth_flows = ["ALLOW_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"]

  access_token_validity = 60
  id_token_validity     = 60

  token_validity_units {
    access_token = "minutes"
    id_token     = "minutes"
  }

  depends_on = [aws_cognito_resource_server.solicitudes]
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

resource "aws_cognito_user" "final" {
  user_pool_id = aws_cognito_user_pool.pool.id
  username     = "Ryuma@gmail.com"
  password     = "Ryuma123456"

  attributes = {
    email          = "Ryuma@gmail.com"
    email_verified = true
    name           = "Ryuma"
  }
  
  message_action = "SUPPRESS"
}

# 5. Declaración de Scopes (Resource Server)
resource "aws_cognito_resource_server" "solicitudes" {
  user_pool_id = aws_cognito_user_pool.pool.id
  identifier   = "solicitudes"
  name         = "API de Solicitudes"

  scope { 
    scope_name = "read" 
    scope_description = "Leer solicitudes" 
  }
  scope { 
    scope_name = "write" 
    scope_description = "Crear, modificar y eliminar solicitudes" 
  }
  scope { 
    scope_name = "approve" 
    scope_description = "Aprobar o rechazar solicitudes" 
  }
}

# 6. Grupos por Rol
resource "aws_cognito_user_group" "solicitantes" {
  name         = "solicitantes"
  user_pool_id = aws_cognito_user_pool.pool.id
  description  = "Puede crear y ver sus solicitudes"
}

resource "aws_cognito_user_group" "aprobadores" {
  name         = "aprobadores"
  user_pool_id = aws_cognito_user_pool.pool.id
  description  = "Puede revisar y aprobar/rechazar solicitudes"
}

# 7. Asignación del Usuario al Grupo Solicitantes
resource "aws_cognito_user_in_group" "demo_solicitante" {
  user_pool_id = aws_cognito_user_pool.pool.id
  username     = aws_cognito_user.demo.username
  group_name   = aws_cognito_user_group.solicitantes.name
}


resource "aws_cognito_user_in_group" "demo_aprovador" {
  user_pool_id = aws_cognito_user_pool.pool.id
  username     = aws_cognito_user.final.username
  group_name   = aws_cognito_user_group.aprobadores.name
}