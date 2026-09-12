# Cluster de ECS
resource "aws_ecs_cluster" "main" {
  name = "dsy1107-cluster"
}

# Referencia al LabRole precreado por el laboratorio de AWS
data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# Security Group para ECS / Backend
resource "aws_security_group" "ecs_sg" {
  name        = "dsy1107-ecs-sg"
  description = "Permitir trafico HTTP al backend"

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# VPC por defecto del lab: Fargate necesita subredes publicas para exponer una
# IP publica y dejarse alcanzar por el API Gateway (sin balanceador).
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "public" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Repositorio de imagenes del backend
resource "aws_ecr_repository" "backend" {
  name                 = "dsy1107-backend-grupo8"
  image_tag_mutability = "MUTABLE"
}

# Grupo de logs del contenedor
resource "aws_cloudwatch_log_group" "ecs_backend" {
  name              = "/ecs/dsy1107-backend-grupo8"
  retention_in_days = 7
}

# Task definition del backend (Fargate)
resource "aws_ecs_task_definition" "backend" {
  family                   = "dsy1107-backend-grupo8"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 512
  memory                   = 1024

  # El Learner Lab no deja crear roles IAM; LabRole sirve para ambas cosas.
  execution_role_arn = data.aws_iam_role.lab_role.arn
  task_role_arn      = data.aws_iam_role.lab_role.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name  = "backend"
      image = "${aws_ecr_repository.backend.repository_url}:latest"
      portMappings = [
        { containerPort = 8080, hostPort = 8080, protocol = "tcp" }
      ]
      environment = [
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/${aws_db_instance.postgres.db_name}" },
        { name = "SPRING_DATASOURCE_USERNAME", value = aws_db_instance.postgres.username },
        { name = "SPRING_DATASOURCE_PASSWORD", value = aws_db_instance.postgres.password },
        { name = "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", value = "https://cognito-idp.${data.aws_region.current.region}.amazonaws.com/${aws_cognito_user_pool.pool.id}" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs_backend.name
          "awslogs-region"        = data.aws_region.current.region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

# Servicio: una sola task con IP publica. La IP va cambiando en cada despliegue,
# por eso publicar-ecs.sh reapunta el API Gateway al terminar.
resource "aws_ecs_service" "backend" {
  name            = "dsy1107-backend-grupo8"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.public.ids
    security_groups  = [aws_security_group.ecs_sg.id]
    assign_public_ip = true
  }

  # publicar-ecs.sh registra revisiones nuevas de la task definition; el apply
  # no debe devolver el servicio a una revision vieja.
  lifecycle {
    ignore_changes = [task_definition]
  }
}