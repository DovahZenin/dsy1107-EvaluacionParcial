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